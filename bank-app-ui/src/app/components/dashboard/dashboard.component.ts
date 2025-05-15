import { Component, OnInit } from '@angular/core';
import { User } from 'src/app/model/user.model';
import {UserDto} from "../../model/userdto.model";
import {UserSessionService} from "../../services/user/user-session.service";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

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
