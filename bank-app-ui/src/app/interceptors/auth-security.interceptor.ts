import { Injectable } from '@angular/core';
import {
  HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpClient, HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { environment } from '../../environments/environment';
import { AppConstants } from '../constants/app.constants';
import { AuthService } from '../services/auth/auth.service';
import { TokenManagerService } from '../services/token/token-manager.service';

interface CsrfToken {
  token: string;
  headerName: string;
}

@Injectable()
export class AuthSecurityInterceptor implements HttpInterceptor {

  constructor(
    private http: HttpClient,
    private tokenManager: TokenManagerService,
    private authService: AuthService
  ) {}

  private fetchCsrfToken(): Observable<CsrfToken> {
    return this.http.get<CsrfToken>(environment.rooturl + AppConstants.CSRF_API_URL, {
      withCredentials: true
    });
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const idsession = sessionStorage.getItem("idsession");
    const isSafeMethod = /^(GET|HEAD|OPTIONS)$/i.test(req.method);

    const isExcluded = [
      AppConstants.EXPIRES_URL,
      AppConstants.REFRESH_URL,
      AppConstants.LOGIN_END_URL,
      AppConstants.LOGOFF_URL,
      AppConstants.CLAIMS_URL,
      AppConstants.SESSION_URL,
      AppConstants.CSRF_API_URL
    ].some(endpoint => req.url.includes(endpoint));

    if (!idsession || isSafeMethod || isExcluded) {
      // No se necesita CSRF ni verificación de sesión
      const safeReq = req.clone({
        withCredentials: true,
        setHeaders: idsession ? { 'X-Idsession': idsession } : {}
      });
      return next.handle(safeReq);
    }

    // Verificamos sesión → obtenemos token CSRF → clonamos con cabeceras
    return this.tokenManager.verificarYRefrescar().pipe(
      switchMap(() => this.fetchCsrfToken()),
      switchMap((csrf) => {
        const headers: Record<string, string> = {
          [csrf.headerName]: csrf.token
        };
        if (idsession) {
          headers['X-Idsession'] = idsession;
        }
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
