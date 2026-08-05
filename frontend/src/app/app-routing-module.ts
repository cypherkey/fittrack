import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';
import { authGuard } from './core/auth.guard';
import { Shell } from './layout/shell/shell';

const routes: Routes = [
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadChildren: () =>
          import('./features/dashboard/dashboard-module').then((m) => m.DashboardModule),
      },
      {
        path: 'workouts',
        loadChildren: () =>
          import('./features/workouts/workouts-module').then((m) => m.WorkoutsModule),
      },
      {
        path: 'templates',
        loadChildren: () =>
          import('./features/templates/templates-module').then((m) => m.TemplatesModule),
      },
      {
        path: 'exercises',
        loadChildren: () =>
          import('./features/exercises/exercises-module').then((m) => m.ExercisesModule),
      },
      {
        path: 'users',
        canActivate: [adminGuard],
        loadChildren: () =>
          import('./features/users/users-module').then((m) => m.UsersModule),
      },
      {
        path: 'settings',
        loadChildren: () =>
          import('./features/settings/settings-module').then((m) => m.SettingsModule),
      },
      { path: 'dashboard', redirectTo: '', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}