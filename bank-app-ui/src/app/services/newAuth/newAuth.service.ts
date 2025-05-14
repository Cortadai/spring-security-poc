import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {AppConstants} from "../../constants/app.constants";

@Injectable({
  providedIn: 'root'
})
export class NewAuthService {

  private refreshTimeout: any;

  constructor(private http: HttpClient) {}

  finalizeLogin(): Observable<void> {
    return this.http.get<void>(environment.rooturl + AppConstants.FIN_LOGIN_URL, { withCredentials: true });
  }


}
