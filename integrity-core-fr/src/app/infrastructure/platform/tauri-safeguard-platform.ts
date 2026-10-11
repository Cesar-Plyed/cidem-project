import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { Injectable } from '@angular/core';
import { SafeguardPlatform, SafeguardPlatformCapabilities, SafeguardNativeViolation } from '../../domain/ports/safeguard-platform';
import { SafeguardEventType } from '../../domain/models/safeguard/safeguard.model';

interface NativeViolationPayload {
  kind: string;
  detail?: string;
}

/**
 * Plataforma Tauri: aporta lo que el navegador no puede.
 *
 * - Al iniciar el examen registra los atajos restringidos a nivel de la app
 *   (global-shortcut captura la combinación: no llega al SO ni a otras apps).
 * - Escucha 'safeguard://native-violation' (emitido por el backend Rust cuando
 *   detecta un cambio de ventana activa) y lo pasa al mismo pipeline del
 *   monitor que las violaciones del navegador.
 * - Al terminar libera todos los atajos y deja el sistema como estaba.
 */
@Injectable({ providedIn: 'root' })
export class TauriSafeguardPlatform extends SafeguardPlatform {
  readonly kind = 'tauri' as const;
  readonly capabilities: SafeguardPlatformCapabilities = {
    nativeFocusTracking: true,
    kioskMode: false, // pendiente: fullscreen de Tauri con fullscreen: true + alwaysOnTop
    osShortcutBlocking: true,
  };

  async enterProtectedMode(): Promise<void> {
    // Nada que hacer todavía de forma global; los atajos los registra el
    // monitor vía blockRestrictedShortcuts() en el momento del start().
    try {
      await invoke('get_system_info');
    } catch (err: unknown) {
      console.warn('Tauri SafeGuard: no se pudo contactar al backend nativo', err);
    }
  }

  async exitProtectedMode(): Promise<void> {
    try {
      await invoke('unblock_all_shortcuts');
    } catch (err: unknown) {
      console.warn('Tauri SafeGuard: no se pudieron liberar los atajos nativos', err);
    }
  }

  onNativeViolation(handler: (violation: SafeguardNativeViolation) => void): () => void {
    let unlisten: (() => void) | null = null;
    let cancelled = false;

    listen<NativeViolationPayload>('safeguard://native-violation', (event) => {
      if (cancelled) return;
      handler({
        type: this.mapViolationKind(event.payload.kind),
        detail: event.payload.detail,
      });
    })
      .then((fn) => {
        if (cancelled) {
          fn();
        } else {
          unlisten = fn;
        }
      })
      .catch((err: unknown) => console.warn('Tauri SafeGuard: no se pudo suscribir a violaciones nativas', err));

    return () => {
      cancelled = true;
      unlisten?.();
    };
  }

  /** Registra un atajo a nivel de app (llama al comando Rust). */
  async blockShortcut(shortcut: string): Promise<boolean> {
    try {
      return await invoke<boolean>('block_keyboard_shortcut', { shortcut });
    } catch (err: unknown) {
      console.warn(`Tauri SafeGuard: fallo al bloquear '${shortcut}'`, err);
      return false;
    }
  }

  private mapViolationKind(kind: string): SafeguardEventType {
    switch (kind) {
      case 'WINDOW_SWITCH':
        return 'NATIVE_WINDOW_SWITCH';
      default:
        return 'NATIVE_WINDOW_SWITCH';
    }
  }
}
