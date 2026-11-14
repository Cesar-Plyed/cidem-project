import { APIRequestContext, Page, request } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

/** URL del backend, configurable para CI o entornos remotos. */
export const API_URL = process.env['E2E_API_URL'] ?? 'http://localhost:8080';

export const PASSWORD = 'E2ePassw0rd!';

export interface E2eUser {
  userId: string;
  email: string;
  password: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

/** Contexto HTTP aislado para preparar datos (registro, login) sin usar la UI. */
export function apiContext(): Promise<APIRequestContext> {
  return request.newContext({
    baseURL: API_URL,
    extraHTTPHeaders: { 'Content-Type': 'application/json' },
  });
}

function uniqueEmail(): string {
  const stamp = Date.now().toString(36);
  const rand = Math.random().toString(36).slice(2, 8);
  return `e2e.${stamp}.${rand}@cidem.test`;
}

/**
 * Registra un alumno nuevo. Cada test usa un correo distinto, así no hay
 * colisiones ni dependencia del orden de ejecución.
 */
export async function registerStudent(api: APIRequestContext): Promise<E2eUser> {
  const countriesResponse = await api.get('/auth/countries');
  if (!countriesResponse.ok()) {
    throw new Error(`No se pudo listar países: HTTP ${countriesResponse.status()}`);
  }
  const countries = (await countriesResponse.json()) as Array<{ id: string }>;
  if (countries.length === 0) {
    throw new Error('El catálogo de países está vacío (¿falta la migración semilla?)');
  }

  const email = uniqueEmail();
  const response = await api.post('/auth/register', {
    data: {
      firstNames: 'E2E',
      lastNames: 'Automatizado',
      email,
      password: PASSWORD,
      countryId: countries[0].id,
      role: 'ALUMNO',
    },
  });

  if (!response.ok()) {
    throw new Error(`Registro falló: HTTP ${response.status()} ${await response.text()}`);
  }

  const body = (await response.json()) as { id: string };
  return { userId: body.id, email, password: PASSWORD };
}

export async function login(api: APIRequestContext, user: E2eUser): Promise<AuthTokens> {
  const response = await api.post('/auth/login', {
    data: { email: user.email, password: user.password },
  });
  if (!response.ok()) {
    throw new Error(`Login falló: HTTP ${response.status()} ${await response.text()}`);
  }
  return (await response.json()) as AuthTokens;
}

/**
 * Inyecta los tokens en localStorage antes de cargar la app. La app lee de
 * ahí (TokenStorageService) y el guard de rol deja pasar al usuario.
 */
export async function seedSession(page: Page, tokens: AuthTokens): Promise<void> {
  await page.addInitScript(
    ([accessToken, refreshToken]) => {
      localStorage.setItem('auth_access_token', accessToken);
      localStorage.setItem('auth_refresh_token', refreshToken);
    },
    [tokens.accessToken, tokens.refreshToken],
  );
}

/** Atajo: registra, loguea y deja la sesión lista para navegar. */
export async function authenticatedPage(page: Page): Promise<E2eUser> {
  const { user, tokens } = loadSharedUser();
  await seedSession(page, tokens);
  return user;
}

/** Ruta del usuario compartido que crea `global-setup.ts`. */
export function sharedUserPath(): string {
  return join(__dirname, '..', '.auth', 'user.json');
}

/** Lee el usuario creado por global setup (evita registros repetidos). */
export function loadSharedUser(): { user: E2eUser; tokens: AuthTokens } {
  const raw = readFileSync(sharedUserPath(), 'utf8');
  return JSON.parse(raw) as { user: E2eUser; tokens: AuthTokens };
}

