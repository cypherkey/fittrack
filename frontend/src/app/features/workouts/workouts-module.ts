import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared-module';
import { TeamWorkoutsPage } from './team-workouts-page/team-workouts-page';
import { WorkoutDetailPage } from './workout-detail-page/workout-detail-page';
import { WorkoutFormPage } from './workout-form-page/workout-form-page';
import { WorkoutsPage } from './workouts-page/workouts-page';
import { WorkoutsRoutingModule } from './workouts-routing-module';

@NgModule({
  declarations: [WorkoutsPage, TeamWorkoutsPage, WorkoutDetailPage, WorkoutFormPage],
  imports: [SharedModule, WorkoutsRoutingModule],
})
export class WorkoutsModule {}
