import { Component, OnInit } from '@angular/core';
import {User} from "../../model/user.model";
import {UserSessionService} from "../../services/user/user-session.service";

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  user = new User();

  constructor(private userSession: UserSessionService) {
  }

  ngOnInit() {
    const userSession = this.userSession.getUserSession();
    if (userSession) {
      this.user = userSession;
    }
  }

}
