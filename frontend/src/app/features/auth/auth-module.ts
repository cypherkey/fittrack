import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { AuthRoutingModule } from './auth-routing-module';
import { AuthCallbackPage } from './auth-callback-page/auth-callback-page';
import { LoginPage } from './login-page/login-page';

@NgModule({
  declarations: [LoginPage, AuthCallbackPage],
  imports: [SharedModule, AuthRoutingModule],
})
export class AuthModule {}
