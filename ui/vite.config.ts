/// <reference types="vitest/config" />
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const api = 'http://localhost:3000'

// The API only allows CORS from http://localhost:5173, so keep the dev port fixed.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    // /packs/{SET}.jpg (pack wrapper art) and /cardback.jpg are also served statically by
    // the API (routes.clj's file handler root); without proxying them the dev server's SPA
    // fallback serves index.html for these paths instead (200, but Content-Type: text/html).
    proxy: { '/api': api, '/setimages': api, '/packs': api, '/cardback.jpg': api },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
})
