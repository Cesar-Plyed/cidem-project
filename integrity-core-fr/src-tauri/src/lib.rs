mod safeguard;
use safeguard::SafeguardState;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .manage(SafeguardState::default())
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
    .plugin(tauri_plugin_global_shortcut::Builder::new().build())
    .setup(|_app| {
      log::info!("Tauri app started - PID: {}", std::process::id());
      log::debug!("Debug logging enabled");
      Ok(())
    })
    .invoke_handler(tauri::generate_handler![
      safeguard::get_system_info,
      safeguard::show_notification,
      safeguard::block_keyboard_shortcut,
      safeguard::unblock_all_shortcuts,
      safeguard::get_active_window_title,
      safeguard::take_screenshot
    ])
    .run(tauri::generate_context!())
    .expect("error while building tauri application");
}
