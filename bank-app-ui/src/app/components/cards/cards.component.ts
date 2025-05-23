import { Component, OnInit } from '@angular/core';
import { Cards } from 'src/app/model/cards.model';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import {UserSessionService} from "../../services/user/user-session.service";
import {User} from "../../model/user.model";


@Component({
  selector: 'app-cards',
  templateUrl: './cards.component.html',
  styleUrls: ['./cards.component.css']
})
export class CardsComponent implements OnInit {

  user = new User();
  cards = new Array();
  currOutstandingAmt:number = 0;

  constructor(private dashboardService: DashboardService,
              private userSession: UserSessionService) { }

  ngOnInit(): void {
    const userSession = this.userSession.getUserSession();
    if (userSession) {
      this.user = userSession;
    }
    if(this.user){
      this.dashboardService.getCardsDetails(this.user.id).subscribe(
        responseData => {
        this.cards = <any> responseData.body;
        this.cards.forEach(function (this: CardsComponent, card: Cards) {
          this.currOutstandingAmt = this.currOutstandingAmt+card.availableAmount;
        }.bind(this));
        });
    }
  }

}
