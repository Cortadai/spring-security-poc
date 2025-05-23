import {createEnvironmentInjector, Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Router} from "@angular/router";
import {environment} from "../../../environments/environment";
import {AppConstants} from "../../constants/app.constants";
import {LogoutService} from "../logout/logout.service";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private refreshTimeout: any;

  constructor(private http: HttpClient, private router: Router, private logoutService: LogoutService) {}

  private parseJwt(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch (error) {
      console.error('Error al decodificar el token', error);
      return null;
    }
  }

  scheduleTokenRefresh(): void {
    const token = sessionStorage.getItem('Authorization');
    if (!token) return;

    const decoded = this.parseJwt(token);
    if (!decoded || !decoded.exp) return;

    const expiresAt = decoded.exp * 1000; // en milisegundos
    const now = Date.now();
    const refreshInMs = expiresAt - now - 10000; // 10 segundos antes

    if (refreshInMs <= 0) {
      this.refreshToken();
      return;
    }

    // Limpia si ya había un timeout pendiente
    clearTimeout(this.refreshTimeout);

    // Programa el refresco
    this.refreshTimeout = setTimeout(() => this.refreshToken(), refreshInMs);
  }

  private refreshToken(): void {
    const refreshToken = sessionStorage.getItem('Authorization-Refresh');
    if (!refreshToken) {
      console.warn('[AuthService] No hay refresh token. Redirigiendo a login.');
      this.router.navigate(['login']);
      return;
    }

    const body = { refreshToken };

    this.http.post(
        environment.rooturl + AppConstants.REFRESH_API_URL,
        body,
        {
          observe: 'response',
          withCredentials: true,
          headers: {
            'Content-Type': 'application/json'
          }
        }
    ).subscribe({
      next: (response) => {
        const newAccessToken = response.headers.get('Authorization');
        const newRefreshToken = response.headers.get('Authorization-Refresh');

        if (newAccessToken && newRefreshToken) {
          sessionStorage.setItem('Authorization', newAccessToken);
          sessionStorage.setItem('Authorization-Refresh', newRefreshToken);
          console.log('[AuthService] Tokens refrescados correctamente.');
          this.scheduleTokenRefresh();
        } else {
          console.warn('[AuthService] No se devolvieron nuevos tokens. Redirigiendo a login.');
          this.router.navigate(['login']);
        }
      },
      error: (err) => {
        console.error('[AuthService] Error al refrescar el token:', err);

        if (err.status === 401) {
          // Limpiar cualquier resto de sesión
          sessionStorage.clear();

          // Feedback al usuario
          alert('Tu sesión ha caducado. Por favor, vuelve a iniciar sesión.');

          // Redirigir a login
          this.router.navigate(['login']);
        } else {
          // Otros errores opcionales
          console.error('Error inesperado durante el refresco:', err);
        }
      }
    });
  }

  logout(): void {
    this.logoutService.logout().subscribe({
      next: (response) => {
        console.log('[AuthService] Logout exitoso en backend:', response);
        this.clearSessionAndRedirect();
      },
      error: (err) => {
        console.warn('[AuthService] Falló el logout en backend, pero limpiamos igual.', err);
        this.clearSessionAndRedirect();
      }
    });
  }

  private clearSessionAndRedirect(): void {
    // Cancelar el timer de refresco
    clearTimeout(this.refreshTimeout);

    // Borrar por completo el sessionStorage
    sessionStorage.clear();

    // Redirigir al login
    this.router.navigate(['/login']);
  }


}
