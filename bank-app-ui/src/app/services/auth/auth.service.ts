import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import {catchError, map, mapTo, switchMap, tap} from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AppConstants } from '../../constants/app.constants';
import { User } from '../../model/user.model';
import {Router} from "@angular/router";

@Injectable({ providedIn: 'root' })
export class AuthService {

  constructor(private http: HttpClient, private router: Router) {}

  private getIdSession(): string {
    const idsession = sessionStorage.getItem('idsession');
    if (!idsession) throw new Error('[AuthService] idsession no encontrada en sessionStorage');
    return idsession;
  }

  private getAuthHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-Idsession': this.getIdSession()
    });
  }

  finalizeLogin(): Observable<void> {
    return this.http.get(environment.rooturl + AppConstants.LOGIN_END_URL, {
      withCredentials: true,
      observe: 'response'
    }).pipe(
      tap(response => {
        const idsession = response.headers.get('X-Idsession');
        if (idsession) {
          sessionStorage.setItem('idsession', idsession);
          console.log('[Login] idsession almacenado:', idsession);
        } else {
          console.warn('[Login] No se recibió X-Idsession en la respuesta.');
        }
      }),
      mapTo(void 0),
      catchError(err => {
        console.error('[Login] Error al finalizar login:', err);
        return throwError(() => new Error('No se pudo completar el login.'));
      })
    );
  }

  loginYObtenerClaims(): Observable<User> {
    return this.finalizeLogin().pipe(
      switchMap(() => this.obtenerClaims())
    );
  }

  logout(): Observable<void> {
    return this.http.get<void>(
      environment.rooturl + AppConstants.LOGOFF_URL,
      {
        withCredentials: true,
        headers: this.getAuthHeaders()
      }
    ).pipe(
      tap(() => {
        console.log('[Logout] Sesión cerrada correctamente');
        sessionStorage.clear();
      }),
      catchError(err => {
        console.error('[Logout] Error al cerrar sesión:', err);
        return throwError(() => new Error('Error al hacer logout'));
      })
    );
  }

  obtenerClaims(): Observable<User> {
    return this.http.get<any>(
      environment.rooturl + AppConstants.CLAIMS_URL,
      {
        withCredentials: true,
        headers: this.getAuthHeaders()
      }
    ).pipe(
      map(claims => {
        const user = this.parseUserFromClaims(claims);
        sessionStorage.setItem('userdetails', JSON.stringify(user));
        return user;
      }),
      catchError(err => {
        console.error('[Claims] Error al obtener claims:', err);
        return throwError(() => new Error('No se pudieron obtener los claims'));
      })
    );
  }

  verificarSesionActiva(): Observable<boolean> {
    return this.http.get(environment.rooturl + AppConstants.SESSION_URL, {
      withCredentials: true,
      observe: 'response',
      headers: this.getAuthHeaders()
    }).pipe(
      map((res: HttpResponse<any>) => res.status === 200),
      catchError(err => {
        console.warn('[Sesion] No activa o error al verificar:', err);
        return of(false);
      })
    );
  }

  cerrarSesionYRedirigir(): void {
    const id = sessionStorage.getItem("idsession");

    if (!id) {
      sessionStorage.clear();
      this.router.navigate(['/home']);
      return;
    }

    this.logout().subscribe({
      next: () => {
        sessionStorage.clear();
        this.router.navigate(['/home']);
      },
      error: () => {
        sessionStorage.clear();
        this.router.navigate(['/home']);
      }
    });
  }

  private parseUserFromClaims(claims: any): User {
    const accessClaims = claims.access;
    const rawName = (accessClaims.sub || '').split('@')[0] || '';
    const formattedName = rawName.charAt(0).toUpperCase() + rawName.slice(1).toLowerCase();
    return new User(
      1,                                  // ID fijo temporal
      formattedName,                      // Nombre
      '528963147',                        // Telf. fijo temporal
      accessClaims.sub || '',             // Email
      accessClaims.roles?.[1] || '',      // Rol (nos quedamos con user a la fuerza por pruebas)
      'AUTH'                              // Estado
    );
  }

}
