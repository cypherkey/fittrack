import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { WorkoutDetailPage } from './workout-detail-page/workout-detail-page';
import { WorkoutFormPage } from './workout-form-page/workout-form-page';
import { WorkoutsPage } from './workouts-page/workouts-page';
import { WorkoutsRoutingModule } from './workouts-routing-module';

@NgModule({
  declarations: [WorkoutsPage, WorkoutDetailPage, WorkoutFormPage],
  imports: [SharedModule, WorkoutsRoutingModule],
})
export class WorkoutsModule {}
