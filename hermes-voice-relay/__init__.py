"""
Hermes Voice Relay Plugin — bridges the Hermes Assistant Android app to Hermes.

Replaces the standalone relay-server.js + desktop-client.js + setup.bat with
a single plugin that runs inside Hermes.

Usage:
  hermes voice-relay start       Start the relay
  hermes voice-relay stop        Stop the relay
  hermes voice-relay status      Show relay status + QR code
  /voice-relay                   Show status (slash command in chat)
"""
from .relay import VoiceRelay

# Singleton — one relay per Hermes process
_relay: VoiceRelay | None = None


def register(ctx):
    """Plugin entry point — called by Hermes plugin system."""
    global _relay
    _relay = VoiceRelay()

    # ── CLI subcommand: hermes voice-relay ──────────────────────────
    ctx.register_cli_command(
        "voice-relay",
        help="Hermes Voice Assistant relay — bridges your Android phone to Hermes",
        setup_fn=_setup_parser,
        handler_fn=_handle_cli,
    )

    # ── Slash command: /voice-relay ─────────────────────────────────
    ctx.register_command(
        name="voice-relay",
        handler=_handle_slash,
        description="Show voice relay QR code and status",
    )

    # ── Hooks: auto-start/stop with Hermes ──────────────────────────
    ctx.register_hook("on_session_start", _on_session_start)
    ctx.register_hook("on_session_end", _on_session_end)


def _setup_parser(subparsers):
    """Build argument parser for `hermes voice-relay`."""
    parser = subparsers.add_parser(
        "voice-relay",
        help="Hermes Voice Assistant relay",
        description="Bridge your Android phone to Hermes via WebSocket relay.",
    )
    sp = parser.add_subparsers(dest="action", required=True)

    sp.add_parser("start", help="Start the voice relay")
    sp.add_parser("stop", help="Stop the voice relay")
    sp.add_parser("status", help="Show relay status and QR code")

    # Advanced options for `start`
    start_p = sp.add_parser("start", help="Start the voice relay")
    start_p.add_argument("--port", type=int, default=8643,
                         help="WebSocket port (default: 8643)")
    start_p.add_argument("--room", type=str, default="HERM",
                         help="Room code for pairing (default: HERM)")
    start_p.add_argument("--token", type=str, default=None,
                         help="Shared secret (auto-generated if not set)")

    return parser


def _handle_cli(args):
    """Handle `hermes voice-relay <action>`."""
    global _relay

    if args.action == "start":
        if _relay.is_running:
            print("[voice-relay] Already running.")
        else:
            # Apply optional overrides
            if hasattr(args, "port"):
                _relay.port = args.port
            if hasattr(args, "room"):
                _relay.room = args.room
            if hasattr(args, "token") and args.token:
                _relay.token = args.token

            _relay.start()
            print("[voice-relay] Started.")

    elif args.action == "stop":
        if _relay.is_running:
            _relay.stop()
            print("[voice-relay] Stopped.")
        else:
            print("[voice-relay] Not running.")

    elif args.action == "status":
        print(_relay.status_text)
        if _relay.is_running:
            _relay._print_banner()


def _handle_slash(args, session):
    """Handle /voice-relay slash command in chat."""
    global _relay
    if _relay is None:
        return "Voice relay plugin not loaded."
    return _relay.status_text


def _on_session_start():
    """Auto-start relay when Hermes session begins."""
    global _relay
    if _relay and not _relay.is_running:
        _relay.start()


def _on_session_end():
    """Auto-stop relay when Hermes session ends."""
    global _relay
    if _relay and _relay.is_running:
        _relay.stop()
