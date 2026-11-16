import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { isTauri } from '@tauri-apps/api/core';
import { authInterceptor } from './auth.interceptor';
import { TokenStorageService } from '../storage/token-storage.service';

vi.mock('@tauri-apps/api/core', () => ({
  invoke: vi.fn(),
  isTauri: vi.fn(),
}));

const isTauriMock = vi.mocked(isTauri);

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: TokenStorageService, useValue: { getAccessToken: async () => 'jwt-token' } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    vi.clearAllMocks();
  });

  it('marca la petición con X-Cidem-Client cuando corre bajo Tauri', async () => {
    isTauriMock.mockReturnValue(true);

    const subscription = http.get('/api/evaluator/exams').subscribe(() => {});
    await new Promise((resolve) => setTimeout(resolve, 0));

    const req = controller.expectOne('/api/evaluator/exams');
    expect(req.request.headers.get('X-Cidem-Client')).toBe('tauri-desktop');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
    subscription.unsubscribe();
  });

  it('no añade el header fuera de Tauri (navegador)', async () => {
    isTauriMock.mockReturnValue(false);

    const subscription = http.get('/api/evaluator/exams').subscribe(() => {});
    await new Promise((resolve) => setTimeout(resolve, 0));

    const req = controller.expectOne('/api/evaluator/exams');
    expect(req.request.headers.get('X-Cidem-Client')).toBeNull();
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
    subscription.unsubscribe();
  });
});
