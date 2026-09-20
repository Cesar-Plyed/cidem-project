import { writeFileSync } from 'node:fs';

const raw = process.env.API_URL;
if (!raw) {
  console.log('[set-api-url] API_URL no definida: se conserva environment.prod.ts');
  process.exit(0);
}

const apiUrl = raw.trim().replace(/\/+$/, '');
if (!/^https?:\/\/[^\s']+$/.test(apiUrl)) {
  console.error(`[set-api-url] API_URL inválida: "${raw}". Debe empezar con http:// o https://`);
  process.exit(1);
}

writeFileSync(
  'src/environments/environment.prod.ts',
  `export const environment = {\n  production: true,\n  apiUrl: '${apiUrl}',\n};\n`,
);
console.log(`[set-api-url] apiUrl = ${apiUrl}`);
