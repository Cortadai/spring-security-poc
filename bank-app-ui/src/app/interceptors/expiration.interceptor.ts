import { inject, Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { AuthService } from '../services/auth/auth.service';
import { AppConstants } from '../constants/app.constants';
import {TokenManagerService} from "../services/token/token-manager.service";
import {Router} from "@angular/router";

@Injectable()
export class ExpirationInterceptor implements HttpInterceptor {

  private tokenManager = inject(TokenManagerService);
  private authService = inject(AuthService);

  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const idsession = sessionStorage.getItem("idsession");

    if (!idsession) {
      return next.handle(req);
    }

    const isExcluded = [
      AppConstants.EXPIRES_URL,
      AppConstants.REFRESH_URL,
      AppConstants.LOGIN_END_URL,
      AppConstants.LOGOFF_URL,
      AppConstants.CLAIMS_URL,
      AppConstants.SESSION_URL,
      AppConstants.CSRF_API_URL
    ].some(endpoint => req.url.includes(endpoint));

    if (isExcluded) {
      return next.handle(req);
    }

    return this.tokenManager.verificarYRefrescar().pipe(
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
