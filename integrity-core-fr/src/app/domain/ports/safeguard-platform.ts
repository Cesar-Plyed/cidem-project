import { SafeguardEventType } from '../models/safeguard/safeguard.model';

/** Qué puede garantizar la plataforma donde corre la app. */
export interface SafeguardPlatformCapabilities {
  /** Detecta perder el foco a nivel de sistema operativo (no solo del navegador). */
  nativeFocusTracking: boolean;
  /** Puede poner la ventana en modo kiosco / pantalla completa bloqueada. */
  kioskMode: boolean;
  /** Puede bloquear atajos del sistema (Alt+Tab, capturas, etc.). */
  osShortcutBlocking: boolean;
}

/** Violación detectada por la capa nativa; entra al mismo flujo que las del navegador. */
export interface SafeguardNativeViolation {
  type: SafeguardEventType;
  detail?: string;
}

/**
 * Abstracción de la plataforma para SafeGuard. Hoy solo existe la versión web
 * (infrastructure/platform/web-safeguard-platform.ts), que no aporta nada nativo.
 *
 * Para Tauri: crear TauriSafeguardPlatform que implemente esta clase (invocando
 * comandos de Rust con @tauri-apps/api) y cambiar el provider en main.ts. El
 * monitor, los casos de uso y la pantalla del examen no se tocan.
 */
export abstract class SafeguardPlatform {
  abstract readonly kind: 'web' | 'tauri';
  abstract readonly capabilities: SafeguardPlatformCapabilities;

  /** Se llama al empezar la protección (Tauri: kiosco, bloquear atajos del SO...). */
  abstract enterProtectedMode(): Promise<void>;

  /** Se llama al terminar; debe dejar el sistema como estaba. */
  abstract exitProtectedMode(): Promise<void>;

  /** Suscripción a violaciones nativas. Devuelve la función para cancelarla. */
  abstract onNativeViolation(handler: (violation: SafeguardNativeViolation) => void): () => void;
}
