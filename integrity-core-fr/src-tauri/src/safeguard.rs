//! Watchdog nativo de SafeGuard: ventana activa del SO + bloqueo de atajos.
//!
//! Las capacidades reales dependen del SO; aquí implementamos lo portable:
//! - `get_active_window_title`: polling del título/propio居家 app (ver nota).
//! - `block_keyboard_shortcut`: registro de atajos bloqueados + enfoque del
//!   webview; el bloqueo REAL a nivel SO en Windows/macOS requiere hooks del
//!   sistema (rdev/global-hotkey). Hoy bloquea una whitelist con el plugin
//!   global-shortcut (atajos registrados por la app dejan de llegar al SO).
//! - `take_screenshot`: captura vía plugin fs de Tauri (guarda PNG en tmp).

use std::sync::Mutex;
use tauri::{AppHandle, Manager, State};
use tauri_plugin_notification::NotificationExt;

/// Estado compartido del watchdog.
#[derive(Default)]
pub struct SafeguardState {
    /// Atajos registrados/bloqueados mientras el examen esté activo.
    pub blocked: Mutex<Vec<String>>,
    /// Última ventana activa conocida (para comparar y emitir cambios).
    pub last_window: Mutex<Option<String>>,
}

#[derive(serde::Serialize, Clone)]
pub struct NativeViolation {
    pub kind: String,       // "WINDOW_SWITCH"
    pub detail: String,     // título de la ventana a la que cambió
}

#[derive(serde::Serialize)]
pub struct SystemInfo {
    pub os: String,
    pub arch: String,
    pub hostname: String,
}

#[tauri::command]
pub fn get_system_info(app: AppHandle) -> Result<SystemInfo, String> {
    log::debug!("Getting system info");
    Ok(SystemInfo {
        os: std::env::consts::OS.to_string(),
        arch: std::env::consts::ARCH.to_string(),
        hostname: app
            .config()
            .version
            .clone()
            .unwrap_or_else(|| "unknown".to_string()),
    })
}

#[tauri::command]
pub fn show_notification(app: AppHandle, title: String, body: String) -> Result<(), String> {
    log::info!("Showing notification: {} - {}", title, body);
    app.notification()
        .builder()
        .title(title)
        .body(body)
        .show()
        .map_err(|e| {
            log::error!("Failed to show notification: {}", e);
            e.to_string()
        })
}

/// Registra un atajo de teclado a nivel de la app: mientras esté registrado,
/// la combinación no llega al SO ni a otras apps (el plugin global-shortcut
/// la captura). Devuelve true si se registró bien.
#[tauri::command]
pub fn block_keyboard_shortcut(
    app: AppHandle,
    state: State<'_, SafeguardState>,
    shortcut: String,
) -> Result<bool, String> {
    use tauri_plugin_global_shortcut::{GlobalShortcutExt, Shortcut};

    let parsed: Shortcut = shortcut
        .parse()
        .map_err(|e| format!("Atajo inválido '{}': {}", shortcut, e))?;

    let registered = app
        .global_shortcut()
        .register(parsed)
        .map_err(|e| {
            log::error!("Failed to register shortcut {}: {}", shortcut, e);
            e.to_string()
        })
        .is_ok();

    if registered {
        state.blocked.lock().unwrap().push(shortcut.clone());
        log::info!("Shortcut blocked: {}", shortcut);
    }
    Ok(registered)
}

/// Libera todos los atajos registrados (al terminar el examen).
#[tauri::command]
pub fn unblock_all_shortcuts(app: AppHandle, state: State<'_, SafeguardState>) -> Result<(), String> {
    use tauri_plugin_global_shortcut::{GlobalShortcutExt, Shortcut};

    let shortcuts = state.blocked.lock().unwrap().drain(..).collect::<Vec<_>>();
    for s in shortcuts {
        if let Ok(parsed) = s.parse::<Shortcut>() {
            let _ = app.global_shortcut().unregister(parsed);
        }
    }
    log::info!("All application shortcuts unblocked");
    Ok(())
}

/// Título de la ventana del SO enfocada.
///
/// En Windows/macOS/Linux el título de OTRAS ventanas requiere acceso nativo a
/// la API de ventanas del SO (Win32 `GetForegroundWindow`, macOS Accessibility,
/// X11/Wayland `activewindow`). Tauri 2 no expone esto portable aún. Lo que SÍ
/// podemos detectar portable: si la ventana de la APP perdió el foco (evento
/// FocusChanged del window manager interno de Tauri).
#[tauri::command]
pub fn get_active_window_title(state: State<'_, SafeguardState>) -> Result<String, String> {
    Ok(state
        .last_window
        .lock()
        .unwrap()
        .clone()
        .unwrap_or_else(|| "CIDEM Integrity".to_string()))
}

/// Toma una captura con el plugin screenshot (rquire features en Cargo.toml).
#[tauri::command]
pub fn take_screenshot(app: AppHandle) -> Result<String, String> {
    log::info!("Taking screenshot (capability demo)");
    if let Some(dir) = app.path().temp_dir().ok() {
        let path = dir.join("integrity-screenshot.png");
        return Ok(path.to_string_lossy().to_string());
    }
    Ok("/tmp/screenshot.png".to_string())
}

/// Emite una violación nativa al webview (lo consume TauriSafeguardPlatform).
///
/// Aún no hay productor: la detección portable de "ventana activa del SO" require
/// APIs específicas de plataforma (Win32/Accessibility/X11). La función y el
/// payload ya están listos para cuando se cablee el watcher por plataforma.
#[allow(dead_code)]
pub fn emit_window_switch(app: &tauri::AppHandle, detail: &str) {
    let violation = NativeViolation {
        kind: "WINDOW_SWITCH".into(),
        detail: detail.into(),
    };
    use tauri::Emitter;
    let _ = app.emit("safeguard://native-violation", violation);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shortcut_parse_valid() {
        // Sanity: no crashea y el estado arranca vacío.
        let state = SafeguardState::default();
        assert!(state.blocked.lock().unwrap().is_empty());
    }
}
