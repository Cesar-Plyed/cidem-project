import { expect, test } from '@playwright/test';
import { loadSharedUser } from './helpers/api';

test.describe('Autenticación', () => {
  test('rechaza credenciales inválidas sin salir del login', async ({ page }) => {
    const { user } = loadSharedUser();

    await page.goto('/login');
    await page.fill('#login-email', user.email);
    await page.fill('#login-password', 'ClaveEquivocada1!');
    await page.click('button[type=submit]');

    await expect(page.locator('.alert-danger')).toContainText(/Correo o contraseña invalid/i);
    await expect(page).toHaveURL(/\/login/);
  });

  test('inicia sesión con credenciales válidas y llega al panel', async ({ page }) => {
    const { user } = loadSharedUser();

    await page.goto('/login');
    await page.fill('#login-email', user.email);
    await page.fill('#login-password', user.password);
    await page.click('button[type=submit]');

    await expect(page).toHaveURL(/\/home/);
    await expect(page.getByRole('link', { name: 'Privacidad y datos' })).toBeVisible();
  });

  test('el guard de rol devuelve al login sin sesión', async ({ page }) => {
    await page.goto('/legal');
    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe('Registro', () => {
  test('crea una cuenta de alumno desde la UI', async ({ page }) => {
    const email = `e2e.ui.${Date.now().toString(36)}@cidem.test`;

    await page.goto('/register');
    await page.fill('#reg-first-names', 'Alumno');
    await page.fill('#reg-last-names', 'DePrueba');
    await page.fill('#reg-email', email);
    await page.fill('#reg-password', 'E2ePassw0rd!');

    // El catálogo de países llega por HTTP: esperamos a que haya opciones reales.
    const country = page.locator('#reg-country');
    await expect(country.locator('option')).toHaveCount(2, { timeout: 15_000 });
    await country.selectOption({ index: 1 });

    await page.click('button[type=submit]');

    await expect(page.locator('.alert-success')).toContainText(/Cuenta creada/i);
    await expect(page).toHaveURL(/\/login/, { timeout: 15_000 });
  });
});
