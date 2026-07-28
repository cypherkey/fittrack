import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthCallbackPage } from './auth-callback-page/auth-callback-page';
import { LoginPage } from './login-page/login-page';
import { guestGuard } from '../../core/auth.guard';

const routes: Routes = [
  { path: 'login', component: LoginPage, canActivate: [guestGuard] },
  { path: 'auth/callback', component: AuthCallbackPage },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AuthRoutingModule {}
