import { SafeguardEventType } from '../../domain/models/safeguard/safeguard.model';

/** Texto que ve el alumno cuando SafeGuard bloquea o detecta algo. */
export function warningMessageFor(type: SafeguardEventType, detail?: string): string {
  switch (type) {
    case 'TAB_HIDDEN':
      return 'Saliste de la pestaña del examen. Esta acción queda registrada.';
    case 'WINDOW_BLUR':
      return 'Perdiste el foco de la ventana del examen. Esta acción queda registrada.';
    case 'FULLSCREEN_EXIT':
      return 'Tu ventana no está en pantalla completa (¿estás en modo dividido?). Vuelve a pantalla completa: la acción queda registrada.';
    case 'COPY_ATTEMPT':
      return 'Copiar está bloqueado durante el examen. El intento queda registrado.';
    case 'CUT_ATTEMPT':
      return 'Cortar está bloqueado durante el examen. El intento queda registrado.';
    case 'PASTE_ATTEMPT':
      return 'Pegar está bloqueado durante el examen. El intento queda registrado.';
    case 'CONTEXT_MENU':
      return 'El menú contextual está deshabilitado durante el examen.';
    case 'RESTRICTED_SHORTCUT':
      return `El atajo ${detail ?? 'de teclado'} está bloqueado durante el examen. El intento queda registrado.`;
    case 'DEVTOOLS_OPENED':
      return 'Se detectó el uso de herramientas de desarrollador. Esta acción queda registrada.';
    case 'PRINT_ATTEMPT':
      return 'Imprimir está bloqueado durante el examen. El intento queda registrado.';
    case 'NATIVE_WINDOW_SWITCH':
      return `Cambiaste a otra aplicación (${detail ?? 'ventana del sistema'}). Esta acción queda registrada.`;
  }
}
