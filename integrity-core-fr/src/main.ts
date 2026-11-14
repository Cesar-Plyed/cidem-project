import { bootstrapApplication } from '@angular/platform-browser';
import { RouteReuseStrategy, provideRouter, withComponentInputBinding, withPreloading, PreloadAllModules } from '@angular/router';
import { IonicRouteStrategy, provideIonicAngular } from '@ionic/angular';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { IonicStorageModule } from '@ionic/storage-angular';
import { importProvidersFrom } from '@angular/core';

import { routes } from './app/app.routes';
import { AppComponent } from './app/app.component';
import { AuthRepository } from './app/domain/ports/auth.repository';
import { AuthHttpRepository } from './app/infrastructure/http/auth-http.repository';
import { authInterceptor } from './app/infrastructure/interceptors/auth.interceptor';
import { refreshInterceptor } from './app/infrastructure/interceptors/refresh.interceptor';
import { ExamRepository } from './app/domain/ports/exam.repository';
import { ExamHttpRepository } from './app/infrastructure/http/exam-http.repository';
import { EvaluatorRepository } from './app/domain/ports/evaluator.repository';
import { EvaluatorHttpRepository } from './app/infrastructure/http/evaluator-http.repository';
import { SafeguardRepository } from './app/domain/ports/safeguard.repository';
import { SafeguardHttpRepository } from './app/infrastructure/http/safeguard-http.repository';
import { OversightRepository } from './app/domain/ports/oversight.repository';
import { OversightHttpRepository } from './app/infrastructure/http/oversight-http.repository';
import { SentinelRepository } from './app/domain/ports/sentinel.repository';
import { SentinelHttpRepository } from './app/infrastructure/http/sentinel-http.repository';
import { SafeguardPlatform } from './app/domain/ports/safeguard-platform';
import { WebSafeguardPlatform } from './app/infrastructure/platform/web-safeguard-platform';
import { TauriSafeguardPlatform } from './app/infrastructure/platform/tauri-safeguard-platform';

// En Tauri ('__TAURI_INTERNALS__' lo inyecta el webview), la plataforma aporta
// capacidades nativas que el navegador no tiene (bloqueo de atajos del SO,
// foco a nivel sistema). En web puro queda la implementación DOM.
const safeguardPlatform =
  '__TAURI_INTERNALS__' in globalThis ? TauriSafeguardPlatform : WebSafeguardPlatform;

bootstrapApplication(AppComponent, {
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    provideIonicAngular(),
    provideRouter(routes, withPreloading(PreloadAllModules), withComponentInputBinding()),
    // authInterceptor va primero (adjunta el token), refreshInterceptor después
    // (reacciona a los 401 y reintenta con el token nuevo).
    provideHttpClient(withInterceptors([authInterceptor, refreshInterceptor])),
    // Conecta el puerto de dominio con su implementación real de infraestructura.
    { provide: AuthRepository, useClass: AuthHttpRepository },
    { provide: ExamRepository, useClass: ExamHttpRepository },
    // Este SÍ pega contra el backend real — pero solo cubre las 5
    // operaciones que existen hoy (ver evaluator-demo.page.ts).
    { provide: EvaluatorRepository, useClass: EvaluatorHttpRepository },
    { provide: SafeguardRepository, useClass: SafeguardHttpRepository },
    // Oversight: los use-cases inyectan la clase abstracta; sin este provider
    // la página /exams/:id/oversight revienta con NG0201 en runtime.
    { provide: OversightRepository, useClass: OversightHttpRepository },
    // Sentinel: pantalla de alertas para profesores (GET/PUT /sentinel/*).
    { provide: SentinelRepository, useClass: SentinelHttpRepository },
    // Plataforma donde corre SafeGuard: Tauri (atajos nativos) o web (solo DOM).
    { provide: SafeguardPlatform, useClass: safeguardPlatform },
    importProvidersFrom(IonicStorageModule.forRoot()),
  ],
});
