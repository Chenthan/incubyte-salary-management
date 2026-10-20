# Frontend development

See the [project README](../README.md) for the Docker-only startup and complete review package.

Use Node 24.19.0 and npm 11.9.0. `npm ci` installs the committed lockfile. `npm start` runs Angular with the API proxy to the Compose application on port 8080.

Checks: `npm test`, `npm run build`, `npm run format:check`, `npm run typecheck:e2e`. For the running app, `npx playwright install chromium` then `npm run e2e`.

To reproduce the recorded demo on a fresh synthetic dataset: install Playwright Chromium/ffmpeg, then run `node e2e/record-demo.mjs`. The recording uses local `.env` credentials without exporting browser session state. Deliberate reading pauses are for the video, not test synchronization.
