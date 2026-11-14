import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env['E2E_BASE_URL'] ?? 'http://localhost:8081';
const apiURL = process.env['E2E_API_URL'] ?? 'http://localhost:8080';

/**
 * E2E de CIDEM Integrity.
 *
 * Requiere el stack levantado (./scripts/dev-up.sh) o, en CI, los servicios
 * equivalentes. Los tests crean sus propios usuarios vía API para no depender
 * de datos precargados.
 */
export default defineConfig({
  testDir: './e2e',
  outputDir: './e2e-results',
  globalSetup: './e2e/global-setup.ts',
  timeout: 45_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  // Un solo worker: los tests comparten backend y base de datos.
  workers: 1,
  retries: process.env['CI'] ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'e2e-results/results.json' }],
  ],
  use: {
    baseURL,
    locale: 'es-MX',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  metadata: { baseURL, apiURL },
});

export { apiURL };
