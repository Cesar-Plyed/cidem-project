import { writeFileSync } from 'node:fs';

const raw = process.env.API_URL;
if (!raw) {
  console.error('[set-tauri-csp] API_URL es obligatoria para empaquetar la aplicación');
  process.exit(1);
}

let api;
try {
  api = new URL(raw);
} catch {
  console.error('[set-tauri-csp] API_URL no es una URL válida');
  process.exit(1);
}

if (api.protocol !== 'https:' || api.username || api.password) {
  console.error('[set-tauri-csp] API_URL debe ser HTTPS y no incluir credenciales');
  process.exit(1);
}

const csp = [
  "default-src 'self' ipc: http://localhost:8080 ws://localhost:8080",
  "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self' data:",
  `connect-src 'self' http://localhost:8080 ws://localhost:8080 ${api.origin} wss://${api.host}`,
].join('; ');

writeFileSync(
  'tauri.alpha.conf.json',
  JSON.stringify({ app: { security: { csp } } }, null, 2) + '\n',
);
