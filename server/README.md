# Nova JARVIS API

A small Node.js 20+ OpenAI-compatible chat proxy. It keeps the server's provider key out of the Android APK and also accepts an optional per-request user key.

## Run locally

1. Copy `.env.example` to `.env` and set `OPENAI_API_KEY`.
2. Set `OPENAI_MODEL` to a model available to your provider.
3. Run `npm start` from this directory.

The service exposes `GET /health` and `POST /v1/chat`.

Example request:

```json
{
  "messages": [
    { "role": "system", "content": "You are Nova, a helpful Android assistant." },
    { "role": "user", "content": "Hello, Jarvis." }
  ]
}
```

For BYOK, send the user's key in the `x-user-api-key` header. The server does not persist or log it.

## Deployment and security

- Deploy behind HTTPS.
- Set `NOVA_ACCESS_TOKEN` for a private deployment and send it as a Bearer token.
- Set `CORS_ORIGIN` to the exact web origin if a web client is used.
- Add durable per-user authentication, quotas, billing controls, and abuse monitoring before public multi-user launch. The included in-memory rate limiter is only a basic development safeguard.
- Never commit `.env`, provider keys, or access tokens.
- A server-side provider key means the app operator pays for usage. It is not an unlimited free AI service.
