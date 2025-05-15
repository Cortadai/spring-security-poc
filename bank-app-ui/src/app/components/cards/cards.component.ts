import { Component, OnInit } from '@angular/core';
import { Cards } from 'src/app/model/cards.model';
import { User } from 'src/app/model/user.model';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import {UserSessionService} from "../../services/user/user-session.service";
import {UserDto} from "../../model/userdto.model";


@Component({
  selector: 'app-cards',
  templateUrl: './cards.component.html',
  styleUrls: ['./cards.component.css']
})
export class CardsComponent implements OnInit {

  userDto = new UserDto();
  cards = new Array();
  currOutstandingAmt:number = 0;

  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const user = this.userSession.getUser();
    if (user) {
      this.userDto = user;
    }
    if(this.userDto){
      this.dashboardService.getCardsDetails(this.userDto.id).subscribe(
        responseData => {
        this.cards = <any> responseData.body;
        this.cards.forEach(function (this: CardsComponent, card: Cards) {
          this.currOutstandingAmt = this.currOutstandingAmt+card.availableAmount;
        }.bind(this));
        });
    }
  }

}
