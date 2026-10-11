import { SafeguardEventType } from '../../domain/models/safeguard/safeguard.model';

const MODIFIER_KEYS = new Set(['Control', 'Meta', 'Alt', 'Shift', 'AltGraph', 'OS']);

/** Atajos de portapapeles: se reportan como copiar/cortar/pegar y no como "atajo restringido". */
export const CLIPBOARD_SHORTCUTS: Readonly<Record<string, SafeguardEventType>> = {
  'ctrl+c': 'COPY_ATTEMPT',
  'ctrl+x': 'CUT_ATTEMPT',
  'ctrl+v': 'PASTE_ATTEMPT',
};

/**
 * Convierte una combinación (de un evento o de la configuración) a una forma canónica en
 * minúsculas: modificadores en orden Ctrl, Alt, Shift y luego la tecla. Cmd/Meta cuenta como
 * Ctrl para que 'Ctrl+C' cubra también macOS. Devuelve null si solo hay modificadores.
 */
export function normalizeShortcut(raw: string): string | null {
  const parts = raw
    .split('+')
    .map((part) => part.trim())
    .filter((part) => part.length > 0);

  const modifiers = { ctrl: false, alt: false, shift: false };
  let key: string | null = null;

  for (const part of parts) {
    const lower = part.toLowerCase();
    if (['ctrl', 'control', 'cmd', 'command', 'meta'].includes(lower)) modifiers.ctrl = true;
    else if (['alt', 'option'].includes(lower)) modifiers.alt = true;
    else if (lower === 'shift') modifiers.shift = true;
    else key = lower;
  }

  return build(modifiers, key);
}

export function shortcutOf(event: Pick<KeyboardEvent, 'key' | 'ctrlKey' | 'metaKey' | 'altKey' | 'shiftKey'>): string | null {
  if (!event.key || MODIFIER_KEYS.has(event.key)) return null;
  return build(
    { ctrl: event.ctrlKey || event.metaKey, alt: event.altKey, shift: event.shiftKey },
    event.key.toLowerCase(),
  );
}

/** Texto legible para mostrar al alumno, por ejemplo 'ctrl+shift+i' -> 'Ctrl+Shift+I'. */
export function prettyShortcut(canonical: string): string {
  return canonical
    .split('+')
    .map((part) => (part.length === 1 ? part.toUpperCase() : part.charAt(0).toUpperCase() + part.slice(1)))
    .join('+');
}

function build(modifiers: { ctrl: boolean; alt: boolean; shift: boolean }, key: string | null): string | null {
  if (!key) return null;
  const parts: string[] = [];
  if (modifiers.ctrl) parts.push('ctrl');
  if (modifiers.alt) parts.push('alt');
  if (modifiers.shift) parts.push('shift');
  parts.push(key);
  return parts.join('+');
}
