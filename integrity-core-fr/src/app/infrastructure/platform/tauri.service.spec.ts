import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { invoke, isTauri } from '@tauri-apps/api/core';
import { TauriService } from './tauri.service';

// Mockea el puente nativo: no hay Tauri en el runner de Vitest (jsdom).
vi.mock('@tauri-apps/api/core', () => ({
  invoke: vi.fn(),
  isTauri: vi.fn(),
}));

const invokeMock = vi.mocked(invoke);
const isTauriMock = vi.mocked(isTauri);

describe('TauriService', () => {
  let service: TauriService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [TauriService] });
    service = TestBed.inject(TauriService);
    vi.clearAllMocks();
  });

  describe('detectWebRestrictions', () => {
    it('success: devuelve true y pasa examId al comando nativo', async () => {
      isTauriMock.mockReturnValue(true);
      invokeMock.mockResolvedValue(true);

      await expect(service.detectWebRestrictions('exam-1')).resolves.toBe(true);
      expect(invokeMock).toHaveBeenCalledWith('detect_web_restrictions', { examId: 'exam-1' });
    });

    it('not-ready (fuera de Tauri): resuelve false sin lanzar unhandled rejection', async () => {
      isTauriMock.mockReturnValue(false);
      // Bajo un navegador, invoke no está disponible y lanza.
      invokeMock.mockRejectedValue(new Error('__TAURI_INTERNALS__ is not defined'));

      await expect(service.detectWebRestrictions('exam-2')).resolves.toBe(false);
    });

    it('error (bajo Tauri): normaliza y relanza para que el llamador lo maneje', async () => {
      isTauriMock.mockReturnValue(true);
      invokeMock.mockRejectedValue(new Error('boom'));

      // La promesa se espera con rejects -> queda manejada, no es unhandled rejection.
      await expect(service.detectWebRestrictions('exam-3')).rejects.toThrow(/boom/);
    });
  });

  describe('hello', () => {
    it('success: devuelve el saludo del backend nativo', async () => {
      isTauriMock.mockReturnValue(true);
      invokeMock.mockResolvedValue('Hello from Tauri');

      await expect(service.hello()).resolves.toBe('Hello from Tauri');
      expect(invokeMock).toHaveBeenCalledWith('hello');
    });

    it('not-ready (fuera de Tauri): resuelve cadena vacía sin lanzar', async () => {
      isTauriMock.mockReturnValue(false);
      invokeMock.mockRejectedValue(new Error('__TAURI_INTERNALS__ is not defined'));

      await expect(service.hello()).resolves.toBe('');
    });
  });
});
