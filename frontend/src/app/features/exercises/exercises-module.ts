import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { ExercisesPage } from './exercises-page/exercises-page';
import { ExercisesRoutingModule } from './exercises-routing-module';

@NgModule({
  declarations: [ExercisesPage],
  imports: [SharedModule, ExercisesRoutingModule],
})
export class ExercisesModule {}
