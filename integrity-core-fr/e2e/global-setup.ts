import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import {
  apiContext,
  login,
  registerStudent,
  sharedUserPath,
} from './helpers/api';

/**
 * Un único usuario por corrida: evita agotar el rate limit de /auth/register
 * y mantiene los tests rápidos. Cada corrida usa un correo nuevo.
 */
export default async function globalSetup(): Promise<void> {
  const api = await apiContext();
  try {
    const user = await registerStudent(api);
    const tokens = await login(api, user);

    const path = sharedUserPath();
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, JSON.stringify({ user, tokens }, null, 2), 'utf8');
  } finally {
    await api.dispose();
  }
}
