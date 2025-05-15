import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SpinnerStateService {
  private _message$ = new BehaviorSubject<string>('Iniciando sesión...');
  message$ = this._message$.asObservable();

  setMessage(msg: string) {
    this._message$.next(msg);
  }
}
