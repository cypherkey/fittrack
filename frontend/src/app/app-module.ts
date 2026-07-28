import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { authInterceptor } from './core/auth.interceptor';
import { CoreModule } from './core/core-module';
import { AuthModule } from './features/auth/auth-module';
import { LayoutModule } from './layout/layout-module';

@NgModule({
  declarations: [App],
  imports: [BrowserModule, CoreModule, LayoutModule, AuthModule, AppRoutingModule],
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
  bootstrap: [App],
})
export class AppModule {}
