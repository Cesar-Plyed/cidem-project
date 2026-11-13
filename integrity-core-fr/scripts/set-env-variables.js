#!/usr/bin/env node
/**
 * set-env-variables.js
 * Inyecta la variable API_URL en el .env de build de Angular antes de ejecutar
 * `npm run build`. Complementa a set-api-url.js (que modifica environment.prod.ts).
 * <p>
 * Uso: `node scripts/set-env-variables.js`
 * </p>
 *
 * @param {string} apiUrl - La URL base del backend (ej. "http://localhost:8080/api")
 */

const fs = require('fs');
const path = require('path');

const envPath = path.join(__dirname, '..', '.env');
const envProdPath = path.join(__dirname, '..', '.env.production');
const envDevPath = path.join(__dirname, '..', '.env.development');

/**
 * Lee API_URL del .env principal o de las vars de entorno.
 * Si no encuentra nada, usa el valor por defecto (localhost:8080).
 */
function getApiUrl() {
  // 1. Try .env.production
  if (fs.existsSync(envProdPath)) {
    const content = fs.readFileSync(envProdPath, 'utf-8');
    const match = content.match(/^API_URL=(.+)$/m);
    if (match) return match[1].trim();
  }

  // 2. Try .env
  if (fs.existsSync(envPath)) {
    const content = fs.readFileSync(envPath, 'utf-8');
    const match = content.match(/^API_URL=(.+)$/m);
    if (match) return match[1].trim();
  }

  // 3. Try process.env
  if (process.env.API_URL) {
    return process.env.API_URL.trim();
  }

  // 4. Fallback
  return 'http://localhost:8080/api';
}

const apiUrl = getApiUrl();
console.log(`API_URL is set to: ${apiUrl}`);

// Validate URL format
if (!/^https?:\/\/[^\s']+/.test(apiUrl)) {
  console.error(`[set-env-variables] API_URL inválida: "${apiUrl}". Debe empezar con http:// o https://`);
  process.exit(1);
}

// Write to .env.production
const prodContent = `API_URL=${apiUrl}\n`;
fs.writeFileSync(envProdPath, prodContent);
console.log(`Wrote ${envProdPath}`);

// Write to .env.development if it exists
if (fs.existsSync(envDevPath)) {
  const devContent = `API_URL=${apiUrl}\n`;
  fs.writeFileSync(envDevPath, devContent);
  console.log(`Wrote ${envDevPath}`);
}

console.log('Build-ready: API_URL is configured.');
