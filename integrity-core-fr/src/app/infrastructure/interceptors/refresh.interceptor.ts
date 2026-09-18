import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, from, switchMap, take, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthRepository } from '../../domain/ports/auth.repository';
import { SessionService } from '../../application/session/session.service';

let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

/**
 * Cuando el access token expira, el backend responde 401. Este
 * interceptor intenta refrescar una sola vez (evitando refrescos en
 * paralelo si varias peticiones fallan al mismo tiempo) y reintenta la
 * petición original con el token nuevo.
 */
export const refreshInterceptor: HttpInterceptorFn = (req, next) => {
  const authRepository = inject(AuthRepository);
  const sessionService = inject(SessionService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthEndpoint = req.url.includes('/auth/');
      if (error.status !== 401 || isAuthEndpoint) {
        return throwError(() => error);
      }

      if (!isRefreshing) {
        isRefreshing = true;
        refreshedToken$.next(null);

        return from(sessionService.getRefreshToken()).pipe(
          switchMap((refreshToken) => {
            if (!refreshToken) {
              isRefreshing = false;
              return handleSessionExpired(sessionService, router, error);
            }

            return authRepository.refresh(refreshToken).pipe(
              switchMap((tokens) => {
                isRefreshing = false;
                sessionService.persistSession(tokens);
                refreshedToken$.next(tokens.accessToken);
                const retryReq = req.clone({
                  setHeaders: { Authorization: `Bearer ${tokens.accessToken}` },
                });
                return next(retryReq);
              }),
              catchError((refreshError) => {
                isRefreshing = false;
                return handleSessionExpired(sessionService, router, refreshError);
              }),
            );
          }),
        );
      }

      // Ya hay un refresh en curso: esperar a que termine y reintentar con el token nuevo
      return refreshedToken$.pipe(
        filter((token) => token !== null),
        take(1),
        switchMap((token) => {
          const retryReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
          return next(retryReq);
        }),
      );
    }),
  );
};

function handleSessionExpired(sessionService: SessionService, router: Router, error: unknown) {
  sessionService.clearSession();
  router.navigate(['/login']);
  return throwError(() => error);
}
