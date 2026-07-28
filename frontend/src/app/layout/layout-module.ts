import { NgModule } from '@angular/core';
import { SharedModule } from '../shared/shared-module';
import { Shell } from './shell/shell';

@NgModule({
  declarations: [Shell],
  imports: [SharedModule],
  exports: [Shell],
})
export class LayoutModule {}
