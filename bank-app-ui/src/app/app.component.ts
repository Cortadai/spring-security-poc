import { Component, OnInit } from '@angular/core';
import { AuthService } from "./services/auth/auth.service";
import { Router } from "@angular/router";
import { NgxSpinnerService } from "ngx-spinner";
import { SpinnerStateService } from "./services/spinnerState/spinner-state.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'bank-app-ui';
  public spinnerMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private spinner: NgxSpinnerService,
    private spinnerStateService: SpinnerStateService
  ) {}

  ngOnInit(): void {
    this.spinnerStateService.message$.subscribe(msg => {
      this.spinnerMessage = msg;
    });

    this.spinner.show();

    const idsession = sessionStorage.getItem("idsession");
    idsession ? this.restaurarSesion() : this.procesarLoginInicial();
  }

  private restaurarSesion(): void {
    this.spinnerStateService.setMessage('Restaurando sesión...');

    this.authService.obtenerClaims().subscribe({
      next: () => {
        setTimeout(() => {
          this.spinner.hide();
          this.router.navigate(['dashboard']);
        }, 1000);
      },
      error: err => {
        console.warn('[App] Error al obtener claims tras recarga:', err);
        this.authService.cerrarSesionYRedirigir();
        this.spinner.hide();
      }
    });
  }

  private procesarLoginInicial(): void {
    this.spinnerStateService.setMessage('Iniciando sesión...');
    this.authService.loginYObtenerClaims().subscribe({
      next: () => {
        setTimeout(() => {
          this.spinner.hide();
          this.router.navigate(['dashboard']);
        }, 1000);
      },
      error: err => {
        console.warn('[App] Error durante login y carga de usuario:', err);
        this.spinner.hide();
        const rutaActual = this.router.url;
        if (rutaActual === '/home') {
          return;
        }
        this.router.navigate(['/forbidden']);
      }
    });
  }

}
