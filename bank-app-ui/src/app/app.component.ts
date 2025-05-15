import {Component, OnInit} from '@angular/core';
import {AuthService} from "./services/auth/auth.service";
import {NewAuthService} from "./services/newAuth/newAuth.service";
import {Router} from "@angular/router";
import {NgxSpinnerService} from "ngx-spinner";
import {SpinnerStateService} from "./services/spinnerState/spinner-state.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit{
  title = 'bank-app-ui';
  public spinnerMessage = '';

  constructor(private newAuthService: NewAuthService,
              private router: Router,
              private spinner: NgxSpinnerService,
              private spinnerStateService: SpinnerStateService) {}

  ngOnInit(): void {
    this.spinnerStateService.message$.subscribe(msg => {
      this.spinnerMessage = msg;
    });

    this.spinner.show();

    this.newAuthService.finalizeLogin().subscribe({
      next: () => {
        this.newAuthService.obtenerClaims().subscribe({
          next: () => {
            setTimeout(() => {
              this.spinner.hide();
              this.router.navigate(['dashboard']);
            }, 1000); // opcional: para que se note la animación
          },
          error: (err) => {
            console.warn('Error al obtener datos de usuario:', err);
            this.spinner.hide();
          }
        });
      },
      error: (err) => {
        console.warn('Error al finalizar login:', err);
        this.spinner.hide();
      }
    });
  }

}
