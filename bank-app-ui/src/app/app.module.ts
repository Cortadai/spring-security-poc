import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {HTTP_INTERCEPTORS, HttpClientModule, HttpClientXsrfModule} from '@angular/common/http';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HeaderComponent } from './components/header/header.component';
import { ContactComponent } from './components/contact/contact.component';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LogoutComponent } from './components/logout/logout.component';
import { NoticesComponent } from './components/notices/notices.component';
import { AccountComponent } from './components/account/account.component';
import { BalanceComponent } from './components/balance/balance.component';
import { LoansComponent } from './components/loans/loans.component';
import { CardsComponent } from './components/cards/cards.component';
import { HomeComponent } from './components/home/home.component';
import {NgxSpinnerModule} from "ngx-spinner";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import { AccessDeniedComponent } from './components/access-denied/access-denied.component';
import {AuthSecurityInterceptor} from "./interceptors/auth-security.interceptor";

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    ContactComponent,
    LoginComponent,
    DashboardComponent,
    LogoutComponent,
    NoticesComponent,
    AccountComponent,
    BalanceComponent,
    LoansComponent,
    CardsComponent,
    HomeComponent,
    AccessDeniedComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    BrowserAnimationsModule,
    NgxSpinnerModule,
    HttpClientModule,
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthSecurityInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {

}
