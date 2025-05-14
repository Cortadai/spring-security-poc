import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {AppConstants} from "../../constants/app.constants";
import {UserDto} from "../../model/userdto.model";
import {tap} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class NewAuthService {

  private refreshTimeout: any;

  constructor(private http: HttpClient) {}

  finalizeLogin(): Observable<void> {
    return this.http.get<void>(environment.rooturl + AppConstants.FIN_LOGIN_URL, { withCredentials: true });
  }

  cargarDatosUsuario(): Observable<UserDto> {
    return this.http.get<UserDto>(environment.rooturl + AppConstants.USER_INFO_URL, { withCredentials: true }).pipe(
      tap((userDto) => {
        sessionStorage.setItem("userdetails", JSON.stringify(userDto));
      })
    );
  }

}
