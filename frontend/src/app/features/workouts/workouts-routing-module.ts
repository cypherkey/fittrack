import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TeamWorkoutsPage } from './team-workouts-page/team-workouts-page';
import { WorkoutDetailPage } from './workout-detail-page/workout-detail-page';
import { WorkoutFormPage } from './workout-form-page/workout-form-page';
import { WorkoutsPage } from './workouts-page/workouts-page';

const routes: Routes = [
  { path: '', component: WorkoutsPage },
  { path: 'team', component: TeamWorkoutsPage },
  { path: 'new', component: WorkoutFormPage },
  { path: ':id/edit', component: WorkoutFormPage },
  { path: ':id', component: WorkoutDetailPage },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class WorkoutsRoutingModule {}
