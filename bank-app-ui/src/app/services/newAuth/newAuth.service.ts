import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {mapTo, Observable} from "rxjs";
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

  cargarDatosUsuario(): Observable<UserDto> {
    return this.http.get<UserDto>(environment.rooturl + AppConstants.USER_INFO_URL, { withCredentials: true }).pipe(
      tap((userDto) => {
        (userDto as any).authStatus = 'AUTH';
        sessionStorage.setItem("userdetails", JSON.stringify(userDto));
      })
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

}
