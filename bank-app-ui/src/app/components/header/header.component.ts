import { Component, OnInit } from '@angular/core';
import { User } from 'src/app/model/user.model';
import {UserDto} from "../../model/userdto.model";
import {UserSessionService} from "../../services/user/user-session.service";

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  userDto = new UserDto();

  constructor(private userSession: UserSessionService) {
  }

  ngOnInit() {
    const user = this.userSession.getUser();
    if (user) {
      this.userDto = user;
    }
  }

}
