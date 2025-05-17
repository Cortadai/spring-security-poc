import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from "@angular/common/http";
import {map, mapTo, Observable, of} from "rxjs";
import {environment} from "../../../environments/environment";
import {AppConstants} from "../../constants/app.constants";
import {catchError, tap} from "rxjs/operators";
import {User} from "../../model/user.model";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  finalizeLogin(): Observable<void> {
    return this.http.get(environment.rooturl + AppConstants.FIN_LOGIN_URL, {
      withCredentials: true,
      observe: 'response'
    }).pipe(
      tap((response) => {
        const idsession = response.headers.get('X-Idsession');
        if (idsession) {
          sessionStorage.setItem("idsession", idsession);
          console.log('[Login] idsession almacenado:', idsession);
        } else {
          console.warn('No se recibió X-Idsession en la respuesta.');
        }
      }),
      mapTo(void 0) // << esta línea convierte HttpResponse<Object> en void
    );
  }

  logout(idsession: string): Observable<void> {
    return this.http.get<void>(
      environment.rooturl + AppConstants.FIN_LOGOFF_URL,
      {
        withCredentials: true,
        headers: { 'X-Idsession': idsession }
      }
    ).pipe(
      tap(() => {
        // Limpiar datos tras logout exitoso
        sessionStorage.clear();
      })
    );
  }

  obtenerClaims(): Observable<User> {
    const idsession = sessionStorage.getItem("idsession");
    if (!idsession) {
      throw new Error("No se encontró idsession en sessionStorage");
    }

    return this.http.get<any>(
      environment.rooturl + AppConstants.OBTENER_CLAIMS_URL,
      {
        withCredentials: true,
        headers: {
          'X-Idsession': idsession
        }
      }
    ).pipe(
      map(claims => {
        const accessClaims = claims.access;
        const rawName = (accessClaims.sub || '').split('@')[0] || '';
        const formattedName = rawName.charAt(0).toUpperCase() + rawName.slice(1).toLowerCase();
        const user = new User(
          1,                                              // id (meto 1 a fuego para pruebas)
          formattedName,                                  // name (o username)
          '528963147',                                    // meto a fuego para pruegas
          accessClaims.sub || '',                        // email
          '',                                             // password
          (accessClaims.roles?.[0] || ''),               // role (primer rol, si hay varios)
          '',                                             // statusCd
          '',                                             // statusMsg
          'AUTH'                                          // authStatus
        );

        // Guardamos en sessionStorage
        sessionStorage.setItem("userdetails", JSON.stringify(user));

        return user;
      })
    );
  }

  verificarSesionActiva(): Observable<boolean> {
    const idsession = sessionStorage.getItem('idsession');
    if (!idsession) return of(false);

    return this.http.get(environment.rooturl + AppConstants.ESTADO_SESION_URL, {
      withCredentials: true,
      observe: 'response',
      headers: { 'X-Idsession': idsession }
    }).pipe(
      map((res: HttpResponse<any>) => res.status === 200),
      catchError(() => of(false))
    );
  }


}
