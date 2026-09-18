import { Injectable } from '@angular/core';
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
  private readonly ready: Promise<void>;

  constructor(private readonly ionicStorage: Storage) {
    this.ready = this.init();
  }

  private async init(): Promise<void> {
    this.storage = await this.ionicStorage.create();
  }

  async saveTokens(accessToken: string, refreshToken: string): Promise<void> {
    await this.ready;
    await this.storage?.set(ACCESS_TOKEN_KEY, accessToken);
    await this.storage?.set(REFRESH_TOKEN_KEY, refreshToken);
  }

  async getAccessToken(): Promise<string | null> {
    await this.ready;
    return (await this.storage?.get(ACCESS_TOKEN_KEY)) ?? null;
  }

  async getRefreshToken(): Promise<string | null> {
    await this.ready;
    return (await this.storage?.get(REFRESH_TOKEN_KEY)) ?? null;
  }

  async clear(): Promise<void> {
    await this.ready;
    await this.storage?.remove(ACCESS_TOKEN_KEY);
    await this.storage?.remove(REFRESH_TOKEN_KEY);
  }
}
