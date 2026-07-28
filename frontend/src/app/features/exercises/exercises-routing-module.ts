import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ExerciseDetailPage } from './exercise-detail-page/exercise-detail-page';
import { ExerciseFormPage } from './exercise-form-page/exercise-form-page';
import { ExercisesPage } from './exercises-page/exercises-page';

const routes: Routes = [
  { path: '', component: ExercisesPage },
  { path: 'new', component: ExerciseFormPage },
  { path: ':id/edit', component: ExerciseFormPage },
  { path: ':id', component: ExerciseDetailPage },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ExercisesRoutingModule {}
