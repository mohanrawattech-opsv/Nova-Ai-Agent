import http from "node:http";

const PORT = Number(process.env.PORT || 8787);
const API_KEY = process.env.OPENAI_API_KEY || "";
const BASE_URL = (process.env.OPENAI_BASE_URL || "https://api.openai.com/v1").replace(/\/$/, "");
const MODEL = process.env.OPENAI_MODEL || "gpt-4.1-mini";
const ACCESS_TOKEN = process.env.NOVA_ACCESS_TOKEN || "";
const WINDOW_MS = 60_000;
const MAX_REQUESTS = 20;
const clients = new Map();

function send(res, status, data) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "access-control-allow-origin": process.env.CORS_ORIGIN || "null",
    "access-control-allow-headers": "content-type, authorization, x-user-api-key",
    "access-control-allow-methods": "GET, POST, OPTIONS"
  });
  res.end(JSON.stringify(data));
}

function allowed(req) {
  if (!ACCESS_TOKEN) return true;
  return req.headers.authorization === `Bearer ${ACCESS_TOKEN}`;
}

function rateAllowed(req) {
  const key = req.socket.remoteAddress || "unknown";
  const now = Date.now();
  const entry = clients.get(key);
  if (!entry || now - entry.start >= WINDOW_MS) {
    clients.set(key, { start: now, count: 1 });
    return true;
  }
  entry.count += 1;
  return entry.count <= MAX_REQUESTS;
}

async function readBody(req) {
  let raw = "";
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 64 * 1024) throw new Error("Request body too large");
  }
  return JSON.parse(raw || "{}");
}

const server = http.createServer(async (req, res) => {
  if (req.method === "OPTIONS") return send(res, 204, {});
  const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);

  if (req.method === "GET" && url.pathname === "/health") {
    return send(res, 200, { ok: true, service: "nova-jarvis-api", configured: Boolean(API_KEY) });
  }
  if (req.method !== "POST" || url.pathname !== "/v1/chat") {
    return send(res, 404, { error: "Not found" });
  }
  if (!allowed(req)) return send(res, 401, { error: "Unauthorized" });
  if (!rateAllowed(req)) return send(res, 429, { error: "Rate limit exceeded. Try again shortly." });

  try {
    const body = await readBody(req);
    const messages = Array.isArray(body.messages) ? body.messages : [];
    if (!messages.length || messages.length > 40 ||
        messages.some(m => !m || !["system", "user", "assistant"].includes(m.role) || typeof m.content !== "string")) {
      return send(res, 400, { error: "messages must contain 1–40 valid chat messages" });
    }

    // A user key is used only for this request and is never stored or logged.
    const key = String(req.headers["x-user-api-key"] || API_KEY).trim();
    if (!key) return send(res, 503, { error: "No AI key configured. Add a server key or provide your own API key." });

    const upstream = await fetch(`${BASE_URL}/chat/completions`, {
      method: "POST",
      headers: { "authorization": `Bearer ${key}`, "content-type": "application/json" },
      body: JSON.stringify({
        model: MODEL,
        messages,
        temperature: 0.6,
        max_tokens: 1200
      }),
      signal: AbortSignal.timeout(45_000)
    });
    const result = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      return send(res, upstream.status, { error: result.error?.message || "AI provider request failed" });
    }
    return send(res, 200, {
      reply: result.choices?.[0]?.message?.content || "",
      model: result.model || MODEL,
      usage: result.usage || null
    });
  } catch (error) {
    const status = error.message === "Request body too large" ? 413 : 400;
    return send(res, status, { error: status === 413 ? error.message : "Unable to process request" });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`Nova JARVIS API listening on port ${PORT}`);
});
