import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {map, mapTo, Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {AppConstants} from "../../constants/app.constants";
import {UserDto} from "../../model/userdto.model";
import {tap} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class NewAuthService {

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

  obtenerClaims(): Observable<UserDto> {
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
        const sessionClaims = claims.session;
        const rawName = (sessionClaims.sub || '').split('@')[0] || '';
        const formattedName = rawName.charAt(0).toUpperCase() + rawName.slice(1).toLowerCase();
        const userDto = new UserDto(
          1,                                              // id (meto 1 a fuego para pruebas)
          formattedName,                                  // name (o username)
          '528963147',                                    // meto a fuego para pruegas
          sessionClaims.sub || '',                        // email
          '',                                             // password
          (sessionClaims.roles?.[0] || ''),               // role (primer rol, si hay varios)
          '',                                             // statusCd
          '',                                             // statusMsg
          'AUTH'                                          // authStatus
        );

        // Guardamos en sessionStorage
        sessionStorage.setItem("userdetails", JSON.stringify(userDto));

        return userDto;
      })
    );
  }


}
