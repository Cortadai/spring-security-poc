import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AppConstants } from '../../constants/app.constants';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, switchMap, mapTo } from 'rxjs/operators';
import {Router} from "@angular/router";

@Injectable({ providedIn: 'root' })
export class TokenManagerService {

  constructor(private http: HttpClient) {}

  private getIdSession(): string {
    const idsession = sessionStorage.getItem("idsession");
    if (!idsession) throw new Error('[TokenManager] idsession no encontrada');
    return idsession;
  }

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-Idsession': this.getIdSession()
    });
  }

  /**
   * Verifica si el token está próximo a expirar y lo refresca si es necesario
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
          return this.http.get<void>(
            environment.rooturl + AppConstants.REFRESH_URL,
            {
              withCredentials: true,
              headers: this.getHeaders()
            }
          ).pipe(mapTo(void 0));
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
