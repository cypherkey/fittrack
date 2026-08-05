import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { SettingsPage } from './settings-page/settings-page';
import { SettingsRoutingModule } from './settings-routing-module';

@NgModule({
  declarations: [SettingsPage],
  imports: [SharedModule, SettingsRoutingModule],
})
export class SettingsModule {}