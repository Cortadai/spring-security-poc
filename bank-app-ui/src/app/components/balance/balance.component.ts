import { Component, OnInit } from '@angular/core';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import {User} from "../../model/user.model";
import {UserSessionService} from "../../services/user/user-session.service";


@Component({
  selector: 'app-balance',
  templateUrl: './balance.component.html',
  styleUrls: ['./balance.component.css']
})
export class BalanceComponent implements OnInit {

  user = new User();
  transactions = new Array();

  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const userSession = this.userSession.getUserSession();
    if (userSession) {
      this.user = userSession;
    }
    if(this.user){
      this.dashboardService.getAccountTransactions(this.user.id).subscribe(
        responseData => {
        this.transactions = <any> responseData.body;
        });
    }
  }

}
