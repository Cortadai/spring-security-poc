import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { User } from 'src/app/model/user.model';
import {AuthService} from "../../services/auth/auth.service";

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html',
  styleUrls: ['./logout.component.css']
})
export class LogoutComponent implements OnInit {

  user = new User();
  constructor(private router : Router, private authService: AuthService) {
  }

  ngOnInit(): void {
    this.authService.logout();
  }


}
