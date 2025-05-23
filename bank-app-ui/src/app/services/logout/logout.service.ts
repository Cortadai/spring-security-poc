import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AppConstants } from '../../constants/app.constants';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LogoutService {

  constructor(private http: HttpClient) {}

  logout(): Observable<any> {
    return this.http.post(
      environment.rooturl + AppConstants.LOGOUT_API_URL, {},
      {
        withCredentials: true,
        responseType: 'text'
      }
    );
  }

}
