import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { ExerciseDetailPage } from './exercise-detail-page/exercise-detail-page';
import { ExerciseFormPage } from './exercise-form-page/exercise-form-page';
import { ExercisesPage } from './exercises-page/exercises-page';
import { ExercisesRoutingModule } from './exercises-routing-module';

@NgModule({
  declarations: [ExercisesPage, ExerciseDetailPage, ExerciseFormPage],
  imports: [SharedModule, ExercisesRoutingModule],
})
export class ExercisesModule {}
