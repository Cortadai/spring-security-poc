import { Component, OnInit } from '@angular/core';
import { Loans } from 'src/app/model/loans.model';
import { User } from 'src/app/model/user.model';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import {UserSessionService} from "../../services/user/user-session.service";
import {UserDto} from "../../model/userdto.model";

@Component({
  selector: 'app-loans',
  templateUrl: './loans.component.html',
  styleUrls: ['./loans.component.css']
})
export class LoansComponent implements OnInit {

  userDto = new UserDto();
  loans = new Array();
  currOutstandingBalance: number = 0;

  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const user = this.userSession.getUser();
    if (user) {
      this.userDto = user;
    }
    if(this.userDto){
      this.dashboardService.getLoansDetails(this.userDto.id).subscribe(
        responseData => {
        this.loans = <any> responseData.body;
        this.loans.forEach(function (this: LoansComponent, loan: Loans) {
          this.currOutstandingBalance = this.currOutstandingBalance+loan.outstandingAmount;
        }.bind(this));
        });
    }
  }



}
