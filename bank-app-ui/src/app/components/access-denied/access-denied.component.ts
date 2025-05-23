import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-access-denied',
  templateUrl: './access-denied.component.html',
  styleUrls: ['./access-denied.component.css']
})
export class AccessDeniedComponent implements OnInit {

  public redireccionInterna = true;
  public mensajeVolver = 'Volver al panel principal';

  ngOnInit(): void {
    const userStr = sessionStorage.getItem('userdetails');
    this.redireccionInterna = !!userStr;

    if (!this.redireccionInterna) {
      this.mensajeVolver = 'Volver al login';
    }
  }

  onVolverClick(): void {
    window.location.href = 'http://localhost:9999/login.html';
  }
}
