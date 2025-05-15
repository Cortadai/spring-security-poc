import { Component, OnInit } from '@angular/core';
import { Loans } from 'src/app/model/loans.model';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import {UserSessionService} from "../../services/user/user-session.service";
import {User} from "../../model/user.model";

@Component({
  selector: 'app-loans',
  templateUrl: './loans.component.html',
  styleUrls: ['./loans.component.css']
})
export class LoansComponent implements OnInit {

  user = new User();
  loans = new Array();
  currOutstandingBalance: number = 0;

  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const userSession = this.userSession.getUserSession();
    if (userSession) {
      this.user = userSession;
    }
    if(this.user){
      this.dashboardService.getLoansDetails(this.user.id).subscribe(
        responseData => {
        this.loans = <any> responseData.body;
        this.loans.forEach(function (this: LoansComponent, loan: Loans) {
          this.currOutstandingBalance = this.currOutstandingBalance+loan.outstandingAmount;
        }.bind(this));
        });
    }
  }



}
