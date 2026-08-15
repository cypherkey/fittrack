import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { AuthService } from '../../../core/auth.service';
import { Workout } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { formatWeight } from '../../../shared/utils/units';

@Component({
  selector: 'app-dashboard-page',
  templateUrl: './dashboard-page.html',
  standalone: false,
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage implements OnInit {
  private readonly workoutApi = inject(WorkoutApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  readonly recentWorkouts = signal<Workout[]>([]);
  readonly loading = signal(false);

  readonly shortcuts = [
    { label: 'Log workout', path: '/workouts/new', icon: 'fitness_center' },
    { label: 'Templates', path: '/templates', icon: 'view_list' },
    { label: 'Exercises', path: '/exercises', icon: 'sports_gymnastics' },
  ];

  ngOnInit(): void {
    this.loading.set(true);
    this.workoutApi.list().subscribe({
      next: (items) => {
        this.recentWorkouts.set(items.slice(0, 10));
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err, 'Failed to load recent workouts'));
      },
    });
  }

  openWorkout(id: string): void {
    void this.router.navigate(['/workouts', id]);
  }

  navigate(path: string): void {
    void this.router.navigate([path]);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '—';
    }
    return new Date(iso).toLocaleString();
  }

  formatTotalWeight(workout: Workout): string {
    return formatWeight(workout.totalWeightLifted, this.auth.user()?.useMetric ?? true);
  }
}
