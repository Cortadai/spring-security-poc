import { Component, OnInit } from '@angular/core';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import { Account } from 'src/app/model/account.model';
import {User} from "../../model/user.model";
import {UserSessionService} from "../../services/user/user-session.service";

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.css']
})
export class AccountComponent implements OnInit {
  user = new User();
  account = new Account();
  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const userSession = this.userSession.getUserSession();
    if (userSession) {
      this.user = userSession;
    }
    if(this.user){
      this.dashboardService.getAccountDetails(this.user.id).subscribe(
        responseData => {
        this.account = <any> responseData.body;
        });
    }

  }

}
