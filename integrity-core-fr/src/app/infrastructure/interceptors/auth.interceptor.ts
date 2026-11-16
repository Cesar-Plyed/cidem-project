import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { inject } from '@angular/core';
import { isTauri } from '@tauri-apps/api/core';
import { TokenStorageService } from '../storage/token-storage.service';

const PUBLIC_PATHS = ['/auth/register', '/auth/login', '/auth/refresh', '/auth/logout', '/auth/countries'];

/**
 * Adjunta el access token a toda petición saliente, excepto a los
 * endpoints públicos de auth (que todavía no tienen sesión que enviar).
 *
 * Además marca las peticiones emitidas desde el cliente de escritorio Tauri
 * con `X-Cidem-Client: tauri-desktop` (spec 51): el backend lo exige para
 * iniciar intentos de exámenes que prohíben el navegador.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const isPublic = PUBLIC_PATHS.some((path) => req.url.includes(path));
  if (isPublic) {
    return next(req);
  }

  const tokenStorage = inject(TokenStorageService);
  const desktopHeaders = isTauri() ? { 'X-Cidem-Client': 'tauri-desktop' } : undefined;

  return from(tokenStorage.getAccessToken()).pipe(
    switchMap((token) => {
      const headers = {
        ...(desktopHeaders ?? {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      };
      const authReq = Object.keys(headers).length > 0 ? req.clone({ setHeaders: headers }) : req;
      return next(authReq);
    }),
  );
};
