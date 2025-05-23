import { Injectable } from '@angular/core';
import {User} from 'src/app/model/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserSessionService {

  private get userDetails(): User | null {
    const raw = sessionStorage.getItem("userdetails");
    return raw ? JSON.parse(raw) as User : null;
  }

  getUserSession(): User | null {
    return this.userDetails;
  }

}
