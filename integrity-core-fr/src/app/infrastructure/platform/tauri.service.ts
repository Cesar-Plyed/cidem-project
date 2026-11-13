import { invoke, isTauri } from '@tauri-apps/api/core';
import { Injectable } from '@angular/core';

/**
 * Puente Angular <-> backend nativo Tauri (spec 22).
 *
 * Encapsula `invoke` de @tauri-apps/api/core y cubre los tres estados posibles:
 *
 * - success: el comando nativo responde -> se devuelve su resultado.
 * - error: el comando nativo falla estando bajo Tauri -> se normaliza y relanza
 *   para que el llamador decida (no se traga el error).
 * - not-ready: el webview NO corre bajo Tauri (frontend servido en navegador
 *   antes/después del backend nativo) -> `invoke` lanza; se trata como default
 *   seguro (false / '') sin romper la app ni producir unhandled rejections.
 */
@Injectable({ providedIn: 'root' })
export class TauriService {
  /**
   * Pregunta al backend nativo si el SO/navegador impone restricciones web para
   * el examen. Fuera de Tauri resuelve `false` en vez de lanzar.
   */
  async detectWebRestrictions(examId: string): Promise<boolean> {
    try {
      return await invoke<boolean>('detect_web_restrictions', { examId });
    } catch (err: unknown) {
      // not-ready: no estamos bajo Tauri -> invoke no existe y lanza.
      if (!isTauri()) {
        console.warn('TauriService: backend Tauri no disponible (not-ready)', err);
        return false;
      }
      // error real del comando nativo -> normalizar y relanzar.
      throw this.normalize(err, 'detect_web_restrictions');
    }
  }

  /**
   * Comando de capacidad mínima: confirma que el puente Tauri responde.
   * Fuera de Tauri resuelve `''` en vez de lanzar.
   */
  async hello(): Promise<string> {
    try {
      return await invoke<string>('hello');
    } catch (err: unknown) {
      if (!isTauri()) {
        console.warn('TauriService: backend Tauri no disponible (not-ready)', err);
        return '';
      }
      throw this.normalize(err, 'hello');
    }
  }

  /** Normaliza cualquier valor lanzado a un `Error` con contexto del comando. */
  private normalize(err: unknown, command: string): Error {
    const message = err instanceof Error ? err.message : String(err);
    return new Error(`TauriService: '${command}' failed: ${message}`);
  }
}
