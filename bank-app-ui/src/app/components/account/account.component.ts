import { Component, OnInit } from '@angular/core';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import { User } from 'src/app/model/user.model';
import { Account } from 'src/app/model/account.model';
import {UserDto} from "../../model/userdto.model";
import {UserSessionService} from "../../services/user/user-session.service";

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.css']
})
export class AccountComponent implements OnInit {
  userDto = new UserDto();
  account = new Account();
  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const user = this.userSession.getUser();
    if (user) {
      this.userDto = user;
    }
    if(this.userDto){
      this.dashboardService.getAccountDetails(this.userDto.id).subscribe(
        responseData => {
        this.account = <any> responseData.body;
        });
    }

  }

}
