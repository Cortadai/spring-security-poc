import { Component, OnInit } from '@angular/core';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import { User } from 'src/app/model/user.model';
import { Account } from 'src/app/model/account.model';
import {UserDto} from "../../model/userdto.model";

@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.css']
})
export class AccountComponent implements OnInit {
  userDto = new UserDto();
  account = new Account();
  constructor(private dashboardService: DashboardService) { }

  ngOnInit(): void {
    this.userDto = JSON.parse(sessionStorage.getItem('userdetails')!);
    if(this.userDto){
      console.log("this.userDto",this.userDto);
      this.dashboardService.getAccountDetails(this.userDto.id).subscribe(
        responseData => {
        this.account = <any> responseData.body;
        });
    }

  }

}
