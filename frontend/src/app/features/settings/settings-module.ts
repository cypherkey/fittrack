import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { SettingsPage } from './settings-page/settings-page';
import { SettingsRoutingModule } from './settings-routing-module';
import { UserFormDialog } from './user-form-dialog/user-form-dialog';

@NgModule({
  declarations: [SettingsPage, UserFormDialog],
  imports: [SharedModule, SettingsRoutingModule],
})
export class SettingsModule {}
