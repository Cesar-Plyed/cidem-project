import { Injectable } from '@angular/core';
import { SafeguardPlatform, SafeguardPlatformCapabilities } from '../../domain/ports/safeguard-platform';

/** Plataforma web: sin capacidades nativas. Toda la protección la hace el monitor con eventos del DOM. */
@Injectable({ providedIn: 'root' })
export class WebSafeguardPlatform extends SafeguardPlatform {
  readonly kind = 'web' as const;
  readonly capabilities: SafeguardPlatformCapabilities = {
    nativeFocusTracking: false,
    kioskMode: false,
    osShortcutBlocking: false,
  };

  async enterProtectedMode(): Promise<void> {
    // Nada que hacer en el navegador.
  }

  async exitProtectedMode(): Promise<void> {
    // Nada que restaurar en el navegador.
  }

  onNativeViolation(): () => void {
    // No hay capa nativa que escuchar; se devuelve una cancelación vacía.
    return () => undefined;
  }
}
