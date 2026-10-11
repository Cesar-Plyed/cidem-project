// Modelos de SafeGuard: configuración del examen, sesión protegida y eventos del monitor.
// Los tipos de evento del frontend se mantienen orientados a la UI; el repositorio HTTP
// los traduce al enum que realmente acepta el backend.

/** Condiciones de riesgo que el monitor web sabe detectar o bloquear. */
export type SafeguardEventType =
  | 'TAB_HIDDEN'
  | 'WINDOW_BLUR'
  | 'FULLSCREEN_EXIT'
  | 'COPY_ATTEMPT'
  | 'CUT_ATTEMPT'
  | 'PASTE_ATTEMPT'
  | 'CONTEXT_MENU'
  | 'RESTRICTED_SHORTCUT'
  /** Se abrieron herramientas de desarrollo (deteción heurística) o se intentó imprimir. */
  | 'DEVTOOLS_OPENED'
  | 'PRINT_ATTEMPT'
  /** Señal nativa (Tauri): cambió la ventana activa del SO. */
  | 'NATIVE_WINDOW_SWITCH';

/** Resultado local de la detección. El backend conserva este dato dentro de metadata. */
export type SafeguardEventResult = 'BLOCKED' | 'DETECTED';

/** Configuración que usa el monitor. Incluye también los campos que entrega el backend. */
export interface SafeguardConfig {
  /** Interruptor general: si es false el monitor no vigila nada. */
  enabled: boolean;

  // Restricciones expuestas por el backend.
  requireCamera: boolean;
  requireMicrophone: boolean;
  requireFullscreen: boolean;
  blockCopyPaste: boolean;
  maxTabSwitches: number;
  riskThreshold: number;
  autoTerminateOnThreshold: boolean;

  // Política del monitor web.
  detectTabSwitch: boolean;
  detectFocusLoss: boolean;
  blockCopy: boolean;
  blockCut: boolean;
  blockPaste: boolean;
  blockContextMenu: boolean;
  blockRestrictedShortcuts: boolean;
  /** Combinaciones como 'Ctrl+P' o 'F12'. En macOS, Cmd cuenta como Ctrl. */
  restrictedShortcuts: string[];
  /** Tiempo mínimo entre dos reportes del mismo tipo de evento. */
  eventThrottleMs: number;
  /** Intervalo de heartbeat del cliente. El backend no lo configura todavía. */
  heartbeatIntervalSeconds: number;
  /** Heurística de devtools (diferencia de tamaño outer/inner anómala). */
  detectDevtools: boolean;
  /** Reporta y bloquea Intento de impresión (beforeprint). */
  blockPrint: boolean;
}

/** Política web por defecto mientras llega la configuración del servidor. */
export const DEFAULT_SAFEGUARD_CONFIG: SafeguardConfig = {
  enabled: true,
  requireCamera: false,
  requireMicrophone: false,
  // Por defecto se exige pantalla completa: una ventana en modo dividido o
  // reducido se reporta como FULLSCREEN_EXIT.
  requireFullscreen: true,
  blockCopyPaste: true,
  maxTabSwitches: 3,
  riskThreshold: 100,
  autoTerminateOnThreshold: false,

  detectTabSwitch: true,
  detectFocusLoss: true,
  blockCopy: true,
  blockCut: true,
  blockPaste: true,
  blockContextMenu: true,
  blockRestrictedShortcuts: true,
  restrictedShortcuts: [
    'Ctrl+C',
    'Ctrl+X',
    'Ctrl+V',
    'Ctrl+P',
    'Ctrl+S',
    'Ctrl+U',
    'Ctrl+Shift+I',
    'Ctrl+Shift+J',
    'Ctrl+Shift+C',
    'F12',
  ],
  eventThrottleMs: 2000,
  heartbeatIntervalSeconds: 15,
  detectDevtools: true,
  blockPrint: true,
};

export type SafeguardSessionStatus =
  | 'PENDING'
  | 'ACTIVE'
  | 'PAUSED'
  | 'COMPLETED'
  | 'FLAGGED'
  | 'TERMINATED';

export interface SafeguardSession {
  sessionId: string;
  examId: string;
  attemptId: string;
  status?: SafeguardSessionStatus;
  riskScore?: number;
  startedAt: string;
  endedAt?: string | null;
  lastHeartbeatAt?: string | null;
  requireCamera?: boolean;
  requireMicrophone?: boolean;
  requireFullscreen?: boolean;
  blockCopyPaste?: boolean;
}

/** Evento local que el repositorio traduce al contrato POST /api/safeguard/events. */
export interface SafeguardEvent {
  /** Lo genera el cliente: el backend lo usa para idempotencia por sesión. */
  eventId: string;
  examId: string;
  attemptId: string;
  /** Siempre debe existir antes de enviarse al backend. */
  sessionId: string;
  type: SafeguardEventType;
  result: SafeguardEventResult;
  occurredAt: string;
  /** Dato extra, por ejemplo la combinación de teclas que se bloqueó. */
  detail?: string;
}

/** Aviso que se le muestra al alumno. */
export interface SafeguardWarning {
  id: number;
  type: SafeguardEventType;
  message: string;
}
