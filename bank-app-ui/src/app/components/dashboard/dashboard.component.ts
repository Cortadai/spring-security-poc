import { Component, OnInit } from '@angular/core';
import { User } from 'src/app/model/user.model';
import {UserDto} from "../../model/userdto.model";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  userDto = new UserDto();

  constructor() {

  }

  ngOnInit() {
    if(sessionStorage.getItem('userdetails')){
      this.userDto = JSON.parse(sessionStorage.getItem('userdetails') || "");
    }
  }

}
