import { Injectable } from '@angular/core';
import { UserDto } from 'src/app/model/userdto.model';

@Injectable({
  providedIn: 'root'
})
export class UserSessionService {

  private get user(): UserDto | null {
    const raw = sessionStorage.getItem("userdetails");
    return raw ? JSON.parse(raw) as UserDto : null;
  }

  getUser(): UserDto | null {
    return this.user;
  }

}
