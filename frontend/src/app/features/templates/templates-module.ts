import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { TemplatesPage } from './templates-page/templates-page';
import { TemplatesRoutingModule } from './templates-routing-module';

@NgModule({
  declarations: [TemplatesPage],
  imports: [SharedModule, TemplatesRoutingModule],
})
export class TemplatesModule {}
