import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { CloneTemplateDialog } from './clone-template-dialog/clone-template-dialog';
import { TemplateDetailPage } from './template-detail-page/template-detail-page';
import { TemplateFormPage } from './template-form-page/template-form-page';
import { TemplatesPage } from './templates-page/templates-page';
import { TemplatesRoutingModule } from './templates-routing-module';

@NgModule({
  declarations: [
    TemplatesPage,
    TemplateDetailPage,
    TemplateFormPage,
    CloneTemplateDialog,
  ],
  imports: [SharedModule, TemplatesRoutingModule],
})
export class TemplatesModule {}
