use tauri::{AppHandle, Manager};
use tauri_plugin_notification::NotificationExt;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .plugin(
      tauri_plugin_log::Builder::default()
        .level(log::LevelFilter::Info)
        .target(tauri_plugin_log::Target::new(
          tauri_plugin_log::TargetKind::Stdout,
        ))
        .target(tauri_plugin_log::Target::new(
          tauri_plugin_log::TargetKind::LogDir { file_name: Some("tauri.log".to_string()) },
        ))
        .target(tauri_plugin_log::Target::new(
          tauri_plugin_log::TargetKind::Webview,
        ))
        .build(),
    )
    .plugin(tauri_plugin_shell::init())
    .plugin(tauri_plugin_dialog::init())
    .plugin(tauri_plugin_notification::init())
    .plugin(tauri_plugin_opener::init())
    .plugin(tauri_plugin_fs::init())
    .setup(|app| {
      log::info!("Tauri app started - PID: {}", std::process::id());
      log::debug!("Debug logging enabled");
      Ok(())
    })
    .invoke_handler(tauri::generate_handler![
      get_system_info,
      show_notification,
      block_keyboard_shortcut,
      get_active_window_title,
      take_screenshot
    ])
    .run(tauri::generate_context!())
    .expect("error while building tauri application");
}

#[tauri::command]
fn get_system_info() -> Result<SystemInfo, String> {
  log::debug!("Getting system info");
  Ok(SystemInfo {
    os: std::env::consts::OS.to_string(),
    arch: std::env::consts::ARCH.to_string(),
    hostname: hostname::get()
      .map(|h| h.to_string_lossy().to_string())
      .unwrap_or_else(|e| {
        log::warn!("Failed to get hostname: {}", e);
        "unknown".to_string()
      }),
  })
}

#[tauri::command]
fn show_notification(app: AppHandle, title: String, body: String) -> Result<(), String> {
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

#[tauri::command]
fn block_keyboard_shortcut(shortcut: String) -> Result<bool, String> {
  log::info!("Attempting to block shortcut: {}", shortcut);
  Ok(true)
}

#[tauri::command]
fn get_active_window_title() -> Result<String, String> {
  log::debug!("Getting active window title");
  Ok("Active Window".to_string())
}

#[tauri::command]
fn take_screenshot() -> Result<String, String> {
  log::info!("Taking screenshot");
  Ok("/tmp/screenshot.png".to_string())
}

#[derive(serde::Serialize)]
struct SystemInfo {
  os: String,
  arch: String,
  hostname: String,
}
