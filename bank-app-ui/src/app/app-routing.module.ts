import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { ContactComponent } from './components/contact/contact.component';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LogoutComponent } from './components/logout/logout.component';
import { AccountComponent } from './components/account/account.component';
import { BalanceComponent } from './components/balance/balance.component';
import { NoticesComponent } from './components/notices/notices.component';
import { LoansComponent } from './components/loans/loans.component';
import { CardsComponent } from './components/cards/cards.component';
import { HomeComponent } from './components/home/home.component';
import {sesionGuard} from "./guards/sesion.guard";
import {rolGuard} from "./guards/rol.guard";
import {AccessDeniedComponent} from "./components/access-denied/access-denied.component";

const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full'},
  { path: 'home', component: HomeComponent},
  { path: 'login', component: LoginComponent},
  { path: 'contact', component: ContactComponent},
  { path: 'notices', component: NoticesComponent},
  { path: 'dashboard', component: DashboardComponent},
  { path: 'logout', component: LogoutComponent},
  { path: 'forbidden', component: AccessDeniedComponent },
  { path: 'myAccount', component: AccountComponent, canActivate: [sesionGuard] },
  { path: 'myBalance', component: BalanceComponent, canActivate: [sesionGuard] },
  { path: 'myLoans', component: LoansComponent, canActivate: [rolGuard] },
  { path: 'myCards', component: CardsComponent,canActivate: [sesionGuard] }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
