import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AppConstants } from '../../constants/app.constants';
import { Observable, of, throwError } from 'rxjs';
import {catchError, map, switchMap, mapTo, tap} from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class TokenManagerService {

  constructor(private http: HttpClient) {}

  private getIdSession(): string {
    const idsession = sessionStorage.getItem("idsession");
    if (!idsession) throw new Error('[TokenManager] idsession no encontrada');
    return idsession;
  }

  private getAccessToken(): string | null {
    return sessionStorage.getItem("access_token");
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders({
      'X-Idsession': this.getIdSession()
    });

    const token = this.getAccessToken();
    if (token) {
      headers = headers.set('Authorization', token);
    }

    return headers;
  }

  /**
   * Verifica si el token está próximo a expirar y lo refresca si es necesario.
   * Requiere que tanto idsession como access_token estén presentes en sessionStorage.
   */
  verificarYRefrescar(): Observable<void> {
    return this.http.get<boolean>(
      environment.rooturl + AppConstants.EXPIRES_URL,
      {
        withCredentials: true,
        headers: this.getHeaders()
      }
    ).pipe(
      switchMap(expira => {
        if (expira) {
          console.log('[TokenManager] Token próximo a expirar, refrescando...');
          return this.http.get(environment.rooturl + AppConstants.REFRESH_URL, {
            withCredentials: true,
            headers: this.getHeaders(),
            observe: 'response'
          }).pipe(
            tap(response => {
              const nuevoToken = response.headers.get('Authorization');
              if (nuevoToken) {
                console.log('[TokenManager] Nuevo token recibido tras refresco');
                sessionStorage.setItem('access_token', nuevoToken);
              } else {
                console.warn('[TokenManager] No se recibió Authorization en refresco');
              }
            }),
            mapTo(void 0)
          );
        } else {
          return of(void 0);
        }
      }),
      catchError(err => {
        console.error('[TokenManager] Error al verificar o refrescar token:', err);
        return throwError(() => err);
      })
    );
  }
}
