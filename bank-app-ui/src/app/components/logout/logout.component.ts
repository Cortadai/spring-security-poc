import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { User } from 'src/app/model/user.model';
import {AuthService} from "../../services/auth/auth.service";
import {NewAuthService} from "../../services/newAuth/newAuth.service";
import {NgxSpinnerService} from "ngx-spinner";
import {SpinnerStateService} from "../../services/spinnerState/spinner-state.service";

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html',
  styleUrls: ['./logout.component.css']
})
export class LogoutComponent implements OnInit {

  user = new User();

  constructor(private router : Router,
              private newAuthService: NewAuthService,
              private spinner: NgxSpinnerService,
              private spinnerStateService: SpinnerStateService) {
  }

  ngOnInit(): void {
    this.spinnerStateService.setMessage('Cerrando sesión...');
    this.spinner.show();

    const idsession = sessionStorage.getItem("idsession");

    if (idsession) {
      this.newAuthService.logout(idsession).subscribe({
        next: () => {
          // Esperamos al menos 1 segundo para que se vea el spinner
          setTimeout(() => {
            this.spinner.hide();
            this.router.navigate(['/home']);
          }, 1000);
        },
        error: err => {
          console.warn('Fallo en el logout, redirigiendo igualmente', err);
          setTimeout(() => {
            this.spinner.hide();
            this.router.navigate(['/home']);
          }, 1000);
        }
      });
    } else {
      setTimeout(() => {
        this.spinner.hide();
        this.router.navigate(['/home']);
      }, 1000);
    }
  }




}
