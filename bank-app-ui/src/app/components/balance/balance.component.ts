import { Component, OnInit } from '@angular/core';
import { User } from 'src/app/model/user.model';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import {UserDto} from "../../model/userdto.model";
import {UserSessionService} from "../../services/user/user-session.service";


@Component({
  selector: 'app-balance',
  templateUrl: './balance.component.html',
  styleUrls: ['./balance.component.css']
})
export class BalanceComponent implements OnInit {

  userDto = new UserDto();
  transactions = new Array();

  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const user = this.userSession.getUser();
    if (user) {
      this.userDto = user;
    }
    if(this.userDto){
      this.dashboardService.getAccountTransactions(this.userDto.id).subscribe(
        responseData => {
        this.transactions = <any> responseData.body;
        });
    }
  }

}
