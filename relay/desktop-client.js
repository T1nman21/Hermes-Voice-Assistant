/**
 * Hermes Voice Desktop Client
 *
 * Bridges the WebSocket relay to Hermes Agent's HTTP API.
 * Connects to the relay as "desktop" role, receives prompts from
 * the phone, forwards them to Hermes, and sends responses back.
 *
 * Usage:
 *   node desktop-client.js --room ABCD                    # connect to relay
 *   node desktop-client.js --room ABCD --relay wss://...  # cloud relay
 *   node desktop-client.js --room ABCD --hermes http://localhost:8642/v1
 *
 * Environment variables:
 *   HERMES_API_URL   — override Hermes API base URL
 *   RELAY_URL        — override relay WebSocket URL
 */

const WebSocket = require("ws");

// ── Config ──────────────────────────────────────────────────────────────
const ROOM = getArg("--room") || "HERM";
const RELAY_URL = process.env.RELAY_URL || getArg("--relay") || "ws://localhost:8643";
const HERMES_URL = process.env.HERMES_API_URL || getArg("--hermes") || "http://localhost:8642/v1";
const HERMES_MODEL = getArg("--model") || "hermes";

function getArg(flag) {
  const idx = process.argv.indexOf(flag);
  return idx >= 0 ? process.argv[idx + 1] : null;
}

// ── State ───────────────────────────────────────────────────────────────
let ws = null;
let reconnectTimer = null;
let reconnectDelay = 1000;

// ── Connect to relay ────────────────────────────────────────────────────
function connect() {
  console.log(`[desktop] Connecting to relay at ${RELAY_URL}...`);
  ws = new WebSocket(RELAY_URL);

  ws.on("open", () => {
    console.log(`[desktop] Connected. Joining room "${ROOM}"`);
    reconnectDelay = 1000;
    ws.send(JSON.stringify({ type: "hello", room: ROOM, role: "desktop" }));
  });

  ws.on("message", async (raw) => {
    let msg;
    try { msg = JSON.parse(raw.toString()); } catch { return; }

    switch (msg.type) {
      case "ready":
        console.log(`[desktop] Joined room "${ROOM}". Waiting for phone...`);
        break;

      case "partner_joined":
        console.log(`[desktop] Phone connected (role: ${msg.role})`);
        break;

      case "partner_left":
        console.log(`[desktop] Phone disconnected`);
        break;

      case "prompt":
        console.log(`[desktop] Received prompt: "${msg.text?.slice(0, 80)}..."`);
        const response = await sendToHermes(msg.text, msg.id);
        if (ws.readyState === 1) {
          ws.send(JSON.stringify({
            type: "response",
            text: response.text,
            id: msg.id,
            error: response.error,
          }));
        }
        break;

      case "pong":
        break;

      default:
        // ignore unknown
    }
  });

  ws.on("close", () => {
    console.log(`[desktop] Disconnected. Reconnecting in ${reconnectDelay / 1000}s...`);
    scheduleReconnect();
  });

  ws.on("error", (err) => {
    console.error(`[desktop] Error: ${err.message}`);
    ws.close();
  });
}

function scheduleReconnect() {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(() => {
    reconnectDelay = Math.min(reconnectDelay * 2, 30_000);
    connect();
  }, reconnectDelay);
}

// ── Hermes API call ─────────────────────────────────────────────────────
async function sendToHermes(text, msgId) {
  try {
    const body = JSON.stringify({
      model: HERMES_MODEL,
      messages: [
        { role: "user", content: text }
      ],
      stream: false,
      max_tokens: 1024,
    });

    const res = await fetch(`${HERMES_URL}/chat/completions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer hermes-local",
      },
      body,
    });

    if (!res.ok) {
      const errText = await res.text().catch(() => "");
      return { text: "", error: `Hermes API error ${res.status}: ${errText.slice(0, 200)}` };
    }

    const data = await res.json();
    const content = data.choices?.[0]?.message?.content || "";
    console.log(`[desktop] Hermes response (${content.length} chars)`);
    return { text: content, error: null };
  } catch (err) {
    return { text: "", error: err.message };
  }
}

// ── Start ───────────────────────────────────────────────────────────────
console.log("=== Hermes Voice Desktop Client ===");
console.log(`Room:      ${ROOM}`);
console.log(`Relay:     ${RELAY_URL}`);
console.log(`Hermes:    ${HERMES_URL}`);
console.log(`Model:     ${HERMES_MODEL}`);
console.log("");
console.log("On your phone, enter this room code: " + ROOM);
console.log("");

// Graceful shutdown
process.on("SIGINT", () => {
  console.log("\n[desktop] Shutting down...");
  if (reconnectTimer) clearTimeout(reconnectTimer);
  if (ws) ws.close();
  process.exit(0);
});

connect();
