import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { inject } from '@angular/core';
import { TokenStorageService } from '../storage/token-storage.service';

const PUBLIC_PATHS = ['/auth/register', '/auth/login', '/auth/refresh', '/auth/logout', '/auth/countries'];

/**
 * Adjunta el access token a toda petición saliente, excepto a los
 * endpoints públicos de auth (que todavía no tienen sesión que enviar).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const isPublic = PUBLIC_PATHS.some((path) => req.url.includes(path));
  if (isPublic) {
    return next(req);
  }

  const tokenStorage = inject(TokenStorageService);

  return from(tokenStorage.getAccessToken()).pipe(
    switchMap((token) => {
      const authReq = token
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;
      return next(authReq);
    }),
  );
};
