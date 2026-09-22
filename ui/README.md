# SWTCG UI

React + TypeScript + Vite frontend for the deck builder API. Plan: `../doc/ui-plan.md`.

- `npm install`, then `npm run dev` (port 5173, required by the API's CORS config).
- Start the API first (port 3000); Vite proxies `/api` and `/setimages` to it.
- `npm test`, `npm run build`, `npm run lint`.
