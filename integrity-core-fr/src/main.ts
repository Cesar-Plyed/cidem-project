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
import { ExamInMemoryRepository } from './app/infrastructure/mock/exam-in-memory.repository';
import { EvaluatorRepository } from './app/domain/ports/evaluator.repository';
import { EvaluatorHttpRepository } from './app/infrastructure/http/evaluator-http.repository';

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
    // TEMPORAL: mientras el backend de exámenes no tenga endpoints de
    // lectura (listar/obtener por id/preguntas), la vista docente/alumno
    // sigue usando este repositorio en memoria.
    { provide: ExamRepository, useClass: ExamInMemoryRepository },
    // Este SÍ pega contra el backend real — pero solo cubre las 5
    // operaciones que existen hoy (ver evaluator-demo.page.ts).
    { provide: EvaluatorRepository, useClass: EvaluatorHttpRepository },
    importProvidersFrom(IonicStorageModule.forRoot()),
  ],
});