import { Injectable, inject } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, switchMap, mapTo } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';
import { AuthService } from '../services/auth/auth.service';
import { environment } from '../../environments/environment';
import { AppConstants } from '../constants/app.constants';

@Injectable()
export class ExpirationInterceptor implements HttpInterceptor {

  private http = inject(HttpClient);
  private router = inject(Router);
  private authService = inject(AuthService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const idsession = sessionStorage.getItem("idsession");

    if (!idsession) {
      return next.handle(req);
    }

    // Evitar bucles con llamadas internas
    if (
      req.url.includes(AppConstants.EXPIRES_URL) ||
      req.url.includes(AppConstants.REFRESH_URL) ||
      req.url.includes(AppConstants.LOGIN_END_URL) ||
      req.url.includes(AppConstants.LOGOFF_URL) ||
      req.url.includes(AppConstants.CLAIMS_URL) ||
      req.url.includes(AppConstants.SESSION_URL)
    ) {
      return next.handle(req);
    }

    return this.http.get<boolean>(environment.rooturl + AppConstants.EXPIRES_URL, {
      withCredentials: true,
      headers: { 'X-Idsession': idsession }
    }).pipe(
      switchMap(expira => {
        if (expira) {
          console.log('[Interceptor] Token por expirar, refrescando...');
          return this.http.get<void>(environment.rooturl + AppConstants.REFRESH_URL, {
            withCredentials: true,
            headers: { 'X-Idsession': idsession }
          }).pipe(mapTo(true));
        } else {
          return of(true);
        }
      }),
      switchMap(() => {
        const modifiedReq = req.clone({
          withCredentials: true,
          setHeaders: { 'X-Idsession': idsession }
        });
        return next.handle(modifiedReq);
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          console.warn('[Interceptor] Token inválido o sin refrescos. Mostrando alerta...');

          Swal.fire({
            icon: 'warning',
            title: 'Sesión expirada',
            text: 'Tu sesión ha caducado. Por favor, vuelve a iniciar sesión.',
            confirmButtonText: 'Aceptar',
            allowOutsideClick: false,
            allowEscapeKey: false
          }).then(() => {
            const id = sessionStorage.getItem("idsession");

            if (id) {
              this.authService.logout(id).subscribe({
                next: () => this.router.navigate(['/home']),
                error: () => this.router.navigate(['/home'])
              });
            } else {
              this.router.navigate(['/home']);
            }
          });
        }

        return throwError(() => error);
      })
    );
  }
}
