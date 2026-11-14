import { expect, test } from '@playwright/test';
import { API_URL, loadSharedUser } from './helpers/api';

test.describe('Contrato y seguridad de la API', () => {
  test('los endpoints legales exigen autenticación', async ({ request }) => {
    const response = await request.get(`${API_URL}/api/legal/consent/history`);
    expect([401, 403]).toContain(response.status());
  });

  test('con token válido los endpoints legales responden 200', async ({ request }) => {
    const { tokens } = loadSharedUser();

    const response = await request.get(`${API_URL}/api/legal/consent/history`, {
      headers: { Authorization: `Bearer ${tokens.accessToken}` },
    });
    expect(response.status()).toBe(200);
  });

  test('la documentación OpenAPI es pública y describe la API', async ({ request }) => {
    const response = await request.get(`${API_URL}/v3/api-docs`);
    expect(response.status()).toBe(200);

    const spec = (await response.json()) as { openapi?: string; paths?: Record<string, unknown> };
    expect(spec.openapi).toBeTruthy();
    expect(Object.keys(spec.paths ?? {}).length).toBeGreaterThan(5);
  });

  test('el health reporta UP', async ({ request }) => {
    const response = await request.get(`${API_URL}/actuator/health`);
    expect(response.status()).toBe(200);
    expect(await response.json()).toMatchObject({ status: 'UP' });
  });
});
