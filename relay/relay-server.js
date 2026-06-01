/**
 * Hermes Voice Relay Server
 *
 * Lightweight WebSocket relay that bridges the Android voice assistant
 * to the Hermes agent running on the desktop. Both the phone and the
 * desktop client connect to this relay and exchange JSON messages.
 *
 * Architecture:
 *   Phone App ──WS──> Relay <──WS── Desktop Client ──HTTP──> Hermes API
 *
 * Usage:
 *   node relay-server.js                                      # defaults
 *   node relay-server.js --port 9000 --room ABCD --token abc  # custom
 *   node relay-server.js --tunnel-url wss://xxx.trycloudflare.com  # with QR
 *
 * Protocol (JSON over WebSocket):
 *   → {"type":"hello","room":"ABCD","role":"phone"|"desktop"}
 *   ← {"type":"ready","room":"ABCD"}
 *   → {"type":"prompt","text":"user's speech","id":"msg-1"}
 *   ← {"type":"response","text":"hermes reply","id":"msg-1","error":null}
 *   ← {"type":"partner_joined"|"partner_left"}
 */

const { WebSocketServer } = require("ws");
const crypto = require("crypto");
const qrcode = require("qrcode-terminal");

// ── CLI args ───────────────────────────────────────────────────────────
function getArg(flag) {
  const idx = process.argv.indexOf(flag);
  return idx >= 0 ? process.argv[idx + 1] : null;
}

const PORT = parseInt(getArg("--port")) || 8643;
const ROOM = getArg("--room") || "HERM";
const TOKEN = getArg("--token") || crypto.randomBytes(8).toString("hex");
const TUNNEL_URL = getArg("--tunnel-url") || null;  // e.g. wss://xxx.trycloudflare.com

// Maps room ID → { phone: ws|null, desktop: ws|null, token: string }
const rooms = new Map();

const wss = new WebSocketServer({ port: PORT });

// ── Startup banner ─────────────────────────────────────────────────────
console.log(`[relay] Hermes Voice Relay listening on ws://0.0.0.0:${PORT}`);
console.log(`[relay] Room: ${ROOM}  |  Secret: ${TOKEN}`);
console.log(`[relay] Desktop: node relay/desktop-client.js --room ${ROOM} --token ${TOKEN}`);

if (TUNNEL_URL) {
  console.log(`[relay] ─────────────────────────────────────────────`);
  console.log(`[relay] Cloudflare Tunnel: ${TUNNEL_URL}`);
  console.log(`[relay] Phone connects from ANYWHERE via the tunnel`);
  console.log(`[relay] ─────────────────────────────────────────────`);
} else {
  console.log(`[relay] ─────────────────────────────────────────────`);
  console.log(`[relay] Local mode (same Wi-Fi): ws://<desktop-ip>:${PORT}`);
  console.log(`[relay] Start with --tunnel-url for remote access via QR`);
  console.log(`[relay] ─────────────────────────────────────────────`);
}

// ── QR Code ────────────────────────────────────────────────────────────
// The QR encodes:  <tunnel-url-or-localhost>|<ROOM>|<TOKEN>
// The app parses this and auto-fills relay URL, room code, and token.
const qrBase = TUNNEL_URL || `ws://localhost:${PORT}`;
const qrData = `${qrBase}|${ROOM}|${TOKEN}`;

setTimeout(() => {
  qrcode.generate(qrData, { small: true }, (qr) => {
    const mode = TUNNEL_URL ? `Cloudflare Tunnel (works from anywhere)` : `local network (same Wi-Fi)`;
    console.log(`\n[relay] ┌─ Scan with Hermes Assistant app ─ ${mode}`);
    console.log(`[relay] │  Or copy: ${qrData}`);
    console.log(qr.split("\n").map(l => `[relay] │ ${l}`).join("\n"));
  });
}, 500);  // small delay so banner prints first

// ── WebSocket handling ─────────────────────────────────────────────────
wss.on("connection", (ws, req) => {
  let roomId = null;
  let role = null;
  let clientId = `${req.socket.remoteAddress}:${req.socket.remotePort}`;

  console.log(`[relay] new connection: ${clientId}`);

  ws.on("message", (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw.toString());
    } catch {
      ws.send(JSON.stringify({ type: "error", message: "invalid JSON" }));
      return;
    }

    switch (msg.type) {
      case "hello": {
        roomId = msg.room;
        role = msg.role;
        const clientToken = msg.token || "";

        if (!roomId || !["phone", "desktop"].includes(role)) {
          ws.send(JSON.stringify({ type: "error", message: "hello requires room and role (phone|desktop)" }));
          return;
        }

        if (clientToken !== TOKEN) {
          ws.send(JSON.stringify({ type: "error", message: "invalid token — check your shared secret" }));
          ws.close();
          return;
        }

        // Join room
        if (!rooms.has(roomId)) {
          rooms.set(roomId, { phone: null, desktop: null, token: TOKEN });
        }
        const room = rooms.get(roomId);

        // If this role is already taken, kick the old one
        const old = room[role];
        if (old && old !== ws && old.readyState === 1) {
          old.send(JSON.stringify({ type: "kicked", reason: "another client took your role" }));
          old.close();
        }

        room[role] = ws;
        console.log(`[relay] ${clientId} joined room ${roomId} as ${role}`);

        ws.send(JSON.stringify({ type: "ready", room: roomId, role }));

        // Notify partner
        const partnerRole = role === "phone" ? "desktop" : "phone";
        const partner = room[partnerRole];
        if (partner && partner.readyState === 1) {
          partner.send(JSON.stringify({ type: "partner_joined", role }));
        }
        break;
      }

      case "prompt": {
        if (!roomId || role !== "phone") {
          ws.send(JSON.stringify({ type: "error", message: "only phone can send prompts" }));
          return;
        }
        const room = rooms.get(roomId);
        const desktop = room?.desktop;
        if (!desktop || desktop.readyState !== 1) {
          ws.send(JSON.stringify({ type: "error", message: "desktop not connected" }));
          return;
        }
        console.log(`[relay] ${clientId} → desktop: "${msg.text?.slice(0, 60)}..."`);
        desktop.send(JSON.stringify({
          type: "prompt",
          text: msg.text || "",
          id: msg.id || "",
        }));
        break;
      }

      case "response": {
        if (!roomId || role !== "desktop") {
          ws.send(JSON.stringify({ type: "error", message: "only desktop can send responses" }));
          return;
        }
        const room = rooms.get(roomId);
        const phone = room?.phone;
        if (!phone || phone.readyState !== 1) {
          console.log(`[relay] phone not connected for room ${roomId}, dropping response`);
          return;
        }
        console.log(`[relay] desktop → phone: "${msg.text?.slice(0, 60)}..."`);
        phone.send(JSON.stringify({
          type: "response",
          text: msg.text || "",
          id: msg.id || "",
          error: msg.error || null,
        }));
        break;
      }

      case "ping": {
        ws.send(JSON.stringify({ type: "pong" }));
        break;
      }

      default: {
        // Forward unknown messages to partner (for extensibility)
        if (roomId) {
          const room = rooms.get(roomId);
          const partnerRole = role === "phone" ? "desktop" : "phone";
          const partner = room?.[partnerRole];
          if (partner && partner.readyState === 1) {
            partner.send(JSON.stringify(msg));
          }
        }
      }
    }
  });

  ws.on("close", () => {
    console.log(`[relay] ${clientId} disconnected (room: ${roomId}, role: ${role})`);
    if (roomId) {
      const room = rooms.get(roomId);
      if (room && room[role] === ws) {
        room[role] = null;
        // Notify partner
        const partnerRole = role === "phone" ? "desktop" : "phone";
        const partner = room[partnerRole];
        if (partner && partner.readyState === 1) {
          partner.send(JSON.stringify({ type: "partner_left", role }));
        }
        // Clean up empty rooms
        if (!room.phone && !room.desktop) {
          rooms.delete(roomId);
          console.log(`[relay] room ${roomId} removed (empty)`);
        }
      }
    }
  });

  ws.on("error", (err) => {
    console.error(`[relay] error on ${clientId}: ${err.message}`);
  });
});

// Periodic cleanup of stale rooms
setInterval(() => {
  for (const [id, room] of rooms) {
    const phoneAlive = room.phone && room.phone.readyState === 1;
    const desktopAlive = room.desktop && room.desktop.readyState === 1;
    if (!phoneAlive && !desktopAlive) {
      rooms.delete(id);
      console.log(`[relay] cleaned up stale room ${id}`);
    }
  }
}, 60_000);

console.log("[relay] Ready. Waiting for connections...");
