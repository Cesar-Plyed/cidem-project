import { writeFileSync } from 'node:fs';

const raw = process.env.API_URL;
if (!raw) {
  if (process.env.VERCEL) {
    console.error('[set-api-url] API_URL es obligatoria para desplegar en Vercel');
    process.exit(1);
  }
  console.log('[set-api-url] API_URL no definida: se conserva environment.prod.ts');
  process.exit(0);
}

const apiUrl = raw.trim().replace(/\/+$/, '');
if (!/^https?:\/\/[^\s']+$/.test(apiUrl)) {
  console.error(`[set-api-url] API_URL inválida: "${raw}". Debe empezar con http:// o https://`);
  process.exit(1);
}
if (process.env.VERCEL && !apiUrl.startsWith('https://')) {
  console.error('[set-api-url] API_URL debe usar HTTPS en despliegues de Vercel');
  process.exit(1);
}

writeFileSync(
  'src/environments/environment.prod.ts',
  `export const environment = {\n  production: true,\n  apiUrl: '${apiUrl}',\n};\n`,
);
console.log(`[set-api-url] apiUrl = ${apiUrl}`);
