import { Injectable } from '@angular/core';
import {
  HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse, HttpResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import {catchError, switchMap, tap} from 'rxjs/operators';
import Swal from 'sweetalert2';
import { AppConstants } from '../constants/app.constants';
import { AuthService } from '../services/auth/auth.service';
import { TokenManagerService } from '../services/token/token-manager.service';

@Injectable()
export class AuthSecurityInterceptor implements HttpInterceptor {

  constructor(
    private tokenManager: TokenManagerService,
    private authService: AuthService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const idsession = sessionStorage.getItem('idsession');
    const token = sessionStorage.getItem('access_token');
    const isSafeMethod = /^(GET|HEAD|OPTIONS)$/i.test(req.method);

    const isExcluded = [
      AppConstants.LOGIN_END_URL,
      AppConstants.EXPIRES_URL,
      AppConstants.REFRESH_URL,
      AppConstants.LOGOFF_URL,
      AppConstants.CLAIMS_URL,
      AppConstants.SESSION_URL
    ].some(endpoint => req.url.includes(endpoint));

    if (!idsession || !token || isSafeMethod || isExcluded) {
      const basicHeaders: Record<string, string> = {
        'X-Token-Pro': '1'
      };
      if (idsession) {
        basicHeaders['X-Idsession'] = idsession;
      }
      if (token) {
        basicHeaders['Authorization'] = token;
      }
      const safeReq = req.clone({
        withCredentials: true,
        setHeaders: basicHeaders
      });
      return next.handle(safeReq);
    }

    return this.tokenManager.verificarYRefrescar().pipe(
      switchMap(() => {
        const refreshedToken = sessionStorage.getItem('access_token');
        const headers: Record<string, string> = {
          'X-Idsession': idsession,
          'Authorization': refreshedToken ?? '',
          'X-Token-Pro': '1'
        };
        const cloned = req.clone({
          withCredentials: true,
          setHeaders: headers
        });
        return next.handle(cloned);
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          console.warn('[Interceptor] Token inválido o sin refrescos. Mostrando alerta...');
          Swal.fire({
            icon: 'warning',
            title: 'Sesión expirada',
            text: 'Por seguridad, se ha cerrado tu sesión.',
            confirmButtonText: 'Aceptar',
            allowOutsideClick: false,
            allowEscapeKey: false
          }).then(() => {
            this.authService.cerrarSesionYRedirigir();
          });
        }
        return throwError(() => error);
      })
    );
  }
}
