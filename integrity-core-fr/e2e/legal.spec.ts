import { expect, test } from '@playwright/test';
import { authenticatedPage } from './helpers/api';

test.describe('Centro de privacidad (/legal)', () => {
  test('muestra el aviso legal y la tabla de consentimientos', async ({ page }) => {
    await authenticatedPage(page);
    await page.goto('/legal');

    await expect(page.getByRole('heading', { name: 'Tus derechos sobre los datos' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Consentimientos', exact: true })).toBeVisible();

    for (const type of ['CAMERA', 'MICROPHONE', 'SCREENSHOT', 'TAB_MONITORING']) {
      await expect(page.getByRole('row', { name: new RegExp(type) })).toBeVisible();
    }
  });

  test('otorga y revoca un consentimiento con historial append-only', async ({ page }) => {
    await authenticatedPage(page);
    await page.goto('/legal');

    const cameraRow = page.getByRole('row', { name: /CAMERA/ });
    await expect(cameraRow).toBeVisible();

    await cameraRow.getByRole('button', { name: 'Otorgar' }).click();
    await expect(page.getByRole('status')).toContainText(/Consentimiento CAMERA otorgado/i);

    const grantedRow = page.getByRole('row', { name: /CAMERA/ });
    await expect(grantedRow).toContainText('granted');

    // El historial debe registrar el cambio (tabla append-only en backend).
    const historyItems = page.locator('ol.small li');
    await expect(historyItems.first()).toContainText(/grant/i);

    await grantedRow.getByRole('button', { name: 'Revocar' }).click();
    await expect(page.getByRole('status')).toContainText(/Consentimiento CAMERA revocado/i);
    await expect(page.getByRole('row', { name: /CAMERA/ })).toContainText('revoked');
  });
});
