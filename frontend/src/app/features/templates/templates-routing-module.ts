import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TemplateDetailPage } from './template-detail-page/template-detail-page';
import { TemplateFormPage } from './template-form-page/template-form-page';
import { TemplatesPage } from './templates-page/templates-page';

const routes: Routes = [
  { path: '', component: TemplatesPage },
  { path: 'new', component: TemplateFormPage },
  { path: ':id/edit', component: TemplateFormPage },
  { path: ':id', component: TemplateDetailPage },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TemplatesRoutingModule {}
