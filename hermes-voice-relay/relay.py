"""
Hermes Voice Relay — async WebSocket bridge between the Android app and Hermes.

Replaces relay-server.js + desktop-client.js. Runs inside Hermes as a plugin.
Phone connects via WebSocket → plugin calls Hermes API → response sent back.
"""
import asyncio
import json
import secrets
import socket
import threading
import urllib.request
from pathlib import Path


class VoiceRelay:
    """Async WebSocket relay that bridges phone prompts to Hermes API."""

    def __init__(self, port: int = 8643, token: str | None = None,
                 room: str = "HERM", hermes_url: str = "http://localhost:8642/v1"):
        self.port = port
        self.token = token or secrets.token_hex(8)
        self.room = room
        self.hermes_url = hermes_url
        self._running = False
        self._thread: threading.Thread | None = None
        self._phone_count = 0  # number of phone clients connected

    # ── public API ─────────────────────────────────────────────────────

    def start(self, blocking: bool = False) -> bool:
        """Start the relay.

        With blocking=True: runs synchronously (for CLI usage).
        With blocking=False: runs in a background daemon thread (for session hooks).
        """
        if self._running:
            return False
        self._running = True
        if blocking:
            asyncio.run(self._serve())
        else:
            self._thread = threading.Thread(target=self._run_loop, daemon=True,
                                            name="hermes-voice-relay")
            self._thread.start()
        return True

    def run_blocking(self):
        """Run the relay synchronously until interrupted. For CLI usage."""
        self.start(blocking=True)

    def stop(self):
        """Stop the relay and disconnect all clients."""
        self._running = False

    @property
    def is_running(self) -> bool:
        return self._running

    @property
    def status_text(self) -> str:
        if not self._running:
            return "Voice relay is stopped. Run /voice-relay to start."
        local_ip = _get_local_ip()
        lines = [
            "🟢 Voice relay is running",
            f"   Port:   {self.port}",
            f"   Room:   {self.room}",
            f"   Token:  {self.token}",
            f"   Phones: {self._phone_count} connected",
            "",
            f"   Local:  ws://{local_ip}:{self.port}",
            f"   QR:     ws://{local_ip}:{self.port}|{self.room}|{self.token}",
        ]
        return "\n".join(lines)

    # ── internals ──────────────────────────────────────────────────────

    def _run_loop(self):
        """Entry point for the background thread."""
        asyncio.run(self._serve())

    async def _serve(self):
        """Run the WebSocket server until stopped."""
        import websockets
        from websockets.asyncio.server import serve

        self._print_banner()
        try:
            async with serve(self._handle_connection, "0.0.0.0", self.port):
                # Keep running, check stop flag periodically
                while self._running:
                    await asyncio.sleep(1)
        except OSError as e:
            print(f"[voice-relay] Failed to bind port {self.port}: {e}")
            self._running = False
        except Exception as e:
            print(f"[voice-relay] Server error: {e}")
            self._running = False

    async def _handle_connection(self, ws):
        """Handle a single WebSocket client (phone)."""
        import websockets

        client_addr = ws.remote_address
        print(f"[voice-relay] Phone connected: {client_addr}")

        try:
            # ── handshake ──────────────────────────────────────────
            raw = await asyncio.wait_for(ws.recv(), timeout=30)
            msg = json.loads(raw)

            if msg.get("type") != "hello":
                await ws.send(json.dumps({"type": "error", "message": "expected hello"}))
                return

            client_token = msg.get("token", "")
            if client_token != self.token:
                await ws.send(json.dumps({"type": "error", "message": "invalid token"}))
                await ws.close()
                return

            role = msg.get("role", "phone")
            room = msg.get("room", self.room)
            print(f"[voice-relay] {client_addr} joined room {room} as {role}")

            # ── ready + partner ─────────────────────────────────────
            await ws.send(json.dumps({"type": "ready", "room": room, "role": role}))
            await ws.send(json.dumps({"type": "partner_joined", "role": "desktop"}))

            self._phone_count += 1

            # ── prompt/response loop ───────────────────────────────
            async for raw_msg in ws:
                if not self._running:
                    break
                try:
                    msg = json.loads(raw_msg)
                except json.JSONDecodeError:
                    continue

                if msg.get("type") == "prompt":
                    text = msg.get("text", "")
                    msg_id = msg.get("id", "")
                    print(f"[voice-relay] Prompt: \"{text[:60]}...\"")

                    response_text, error = await self._call_hermes(text)
                    await ws.send(json.dumps({
                        "type": "response",
                        "text": response_text,
                        "id": msg_id,
                        "error": error,
                    }))

                elif msg.get("type") == "ping":
                    await ws.send(json.dumps({"type": "pong"}))

        except asyncio.TimeoutError:
            pass  # client didn't send hello in time
        except websockets.exceptions.ConnectionClosed:
            pass  # normal disconnect
        except Exception as e:
            print(f"[voice-relay] Error with {client_addr}: {e}")
        finally:
            self._phone_count = max(0, self._phone_count - 1)
            print(f"[voice-relay] Phone disconnected: {client_addr}")

    async def _call_hermes(self, prompt: str) -> tuple[str, str | None]:
        """Send a prompt to the Hermes API and return (text, error)."""
        try:
            body = json.dumps({
                "model": "hermes",
                "messages": [{"role": "user", "content": prompt}],
                "stream": False,
                "max_tokens": 1024,
            }).encode("utf-8")

            req = urllib.request.Request(
                f"{self.hermes_url}/chat/completions",
                data=body,
                headers={
                    "Content-Type": "application/json",
                    "Authorization": "Bearer hermes-local",
                },
            )

            # Run blocking HTTP call in executor to avoid blocking the event loop
            loop = asyncio.get_running_loop()
            resp = await loop.run_in_executor(None, urllib.request.urlopen, req)

            data = json.loads(resp.read().decode("utf-8"))
            content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
            print(f"[voice-relay] Hermes response ({len(content)} chars)")
            return content, None

        except urllib.error.URLError as e:
            return "", f"Hermes API unreachable: {e.reason}"
        except Exception as e:
            return "", str(e)

    def _print_banner(self):
        """Print startup banner with QR code."""
        local_ip = _get_local_ip()
        qr_data = f"ws://{local_ip}:{self.port}|{self.room}|{self.token}"

        print()
        print("=" * 60)
        print("  Hermes Voice Relay")
        print("=" * 60)
        print(f"  Port:  {self.port}")
        print(f"  Room:  {self.room}")
        print(f"  Token: {self.token}")
        print(f"  Local: ws://{local_ip}:{self.port}")
        print()
        print("  Scan with Hermes Assistant app:")
        print()

        try:
            import qrcode
            qr = qrcode.QRCode(border=1)
            qr.add_data(qr_data)
            qr.make(fit=True)
            qr.print_ascii(invert=True)
        except ImportError:
            # Fallback: print the raw QR data
            print(f"  (Install qrcode for visual QR: pip install qrcode)")
            print(f"  {qr_data}")

        print()
        print(f"  Or copy: {qr_data}")
        print("=" * 60)
        print()


# ── helpers ─────────────────────────────────────────────────────────────

def _get_local_ip() -> str:
    """Get the local network IP address."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "localhost"
