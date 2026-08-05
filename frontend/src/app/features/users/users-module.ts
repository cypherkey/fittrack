import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { UserFormDialog } from './user-form-dialog/user-form-dialog';
import { UsersPage } from './users-page/users-page';
import { UsersRoutingModule } from './users-routing-module';

@NgModule({
  declarations: [UsersPage, UserFormDialog],
  imports: [SharedModule, UsersRoutingModule],
})
export class UsersModule {}