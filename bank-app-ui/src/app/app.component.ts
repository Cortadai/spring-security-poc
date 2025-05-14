import {Component, OnInit} from '@angular/core';
import {AuthService} from "./services/auth/auth.service";
import {NewAuthService} from "./services/newAuth/newAuth.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit{
  title = 'bank-app-ui';

  constructor(private authService: AuthService, private newAuthService: NewAuthService) {}

  ngOnInit(): void {
    // this.authService.scheduleTokenRefresh();
    this.newAuthService.finalizeLogin().subscribe({
      next: () => console.log('Login finalizado correctamente'),
      error: (err) => console.warn('Error al finalizar login:', err)
    });
  }




}
