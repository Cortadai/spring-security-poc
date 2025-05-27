import { Injectable } from '@angular/core';
import {
  HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpClient
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AppConstants } from '../constants/app.constants';

interface CsrfToken {
  token: string;
  headerName: string;
}

@Injectable()
export class CsrfInterceptor implements HttpInterceptor {

  constructor(private http: HttpClient) {}

  private fetchCsrfToken(): Observable<CsrfToken> {
    return this.http.get<CsrfToken>(environment.rooturl + AppConstants.CSRF_API_URL, {
      withCredentials: true
    });
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isSafeMethod = /^(GET|HEAD|OPTIONS)$/i.test(req.method);

    // Si es un método seguro, no necesitas CSRF
    if (isSafeMethod) {
      return next.handle(req);
    }

    return this.fetchCsrfToken().pipe(
      switchMap(csrf => {
        const cloned = req.clone({
          withCredentials: true,
          headers: req.headers.set(csrf.headerName, csrf.token)
        });
        return next.handle(cloned);
      }),
      catchError(err => throwError(() => err))
    );
  }
}
