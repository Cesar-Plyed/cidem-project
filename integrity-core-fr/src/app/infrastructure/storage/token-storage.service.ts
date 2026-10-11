import { inject, Injectable } from '@angular/core';
import { Storage } from '@ionic/storage-angular';

const ACCESS_TOKEN_KEY = 'auth_access_token';
const REFRESH_TOKEN_KEY = 'auth_refresh_token';

/**
 * Encapsula el detalle de "dónde" se guardan los tokens (Ionic Storage:
 * IndexedDB en web, SQLite/nativo en apps empaquetadas). Nadie fuera de
 * infrastructure/ debería saber que esto existe.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private storage: Storage | null = null;
  private readyPromise: Promise<void> | null = null;
  private readonly ionicStorage = inject(Storage);

  /**
   * Inicialización perezosa: Ionic Storage solo se crea en el primer uso, así
   * el constructor no dispara trabajo asíncrono (y el servicio se puede
   * instanciar sin coste hasta que alguien guarda o lee un token).
   */
  private get ready(): Promise<void> {
    if (this.readyPromise === null) {
      this.readyPromise = this.init();
    }
    return this.readyPromise;
  }

  private async init(): Promise<void> {
    this.storage = await this.ionicStorage.create();
  }

  async saveTokens(accessToken: string, refreshToken: string): Promise<void> {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    await this.ready;
    await this.storage?.set(ACCESS_TOKEN_KEY, accessToken);
    await this.storage?.set(REFRESH_TOKEN_KEY, refreshToken);
  }

  async getAccessToken(): Promise<string | null> {
    const cachedToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (cachedToken) return cachedToken;
    await this.ready;
    return (await this.storage?.get(ACCESS_TOKEN_KEY)) ?? null;
  }

  async getRefreshToken(): Promise<string | null> {
    const cachedToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (cachedToken) return cachedToken;
    await this.ready;
    return (await this.storage?.get(REFRESH_TOKEN_KEY)) ?? null;
  }

  async clear(): Promise<void> {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    await this.ready;
    await this.storage?.remove(ACCESS_TOKEN_KEY);
    await this.storage?.remove(REFRESH_TOKEN_KEY);
  }
}
