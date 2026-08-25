import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { AuthService } from '../../../core/auth.service';
import { Exercise, ExerciseHistoryEntry } from '../../../core/models/exercise';
import { Workout } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { WeightProgressSeries } from '../../../shared/utils/weight-progress-chart';
import { formatWeight } from '../../../shared/utils/units';

@Component({
  selector: 'app-dashboard-page',
  templateUrl: './dashboard-page.html',
  standalone: false,
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage implements OnInit {
  private readonly workoutApi = inject(WorkoutApi);
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  readonly recentWorkouts = signal<Workout[]>([]);
  readonly loadingWorkouts = signal(false);

  readonly favoriteExercises = signal<Exercise[]>([]);
  readonly chartSeries = signal<WeightProgressSeries[]>([]);
  readonly loadingCharts = signal(false);

  readonly favoritesEmpty = computed(() => this.favoriteExercises().length === 0);

  readonly shortcuts = [
    { label: 'Log workout', path: '/workouts/new', icon: 'fitness_center' },
    { label: 'Templates', path: '/templates', icon: 'view_list' },
    { label: 'Exercises', path: '/exercises', icon: 'sports_gymnastics' },
  ];

  ngOnInit(): void {
    this.loadRecentWorkouts();
    this.loadFavoriteCharts();
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

  private loadRecentWorkouts(): void {
    this.loadingWorkouts.set(true);
    this.workoutApi.list().subscribe({
      next: (items) => {
        this.recentWorkouts.set(items.slice(0, 5));
        this.loadingWorkouts.set(false);
      },
      error: (err) => {
        this.loadingWorkouts.set(false);
        this.notify.error(errorMessage(err, 'Failed to load recent workouts'));
      },
    });
  }

  private loadFavoriteCharts(): void {
    this.loadingCharts.set(true);
    this.exerciseApi
      .list({ size: 500, favoriteOnly: true })
      .pipe(
        map((page) => page.content),
        switchMap((favorites) => {
          this.favoriteExercises.set(favorites);
          if (favorites.length === 0) {
            return of([] as ExerciseHistoryEntry[][]);
          }
          return forkJoin(favorites.map((exercise) => this.exerciseApi.history(exercise.id)));
        }),
      )
      .subscribe({
        next: (histories) => {
          const favorites = this.favoriteExercises();
          this.chartSeries.set(
            favorites.map((exercise, i) => ({
              label: exercise.name,
              history: histories[i] ?? [],
            })),
          );
          this.loadingCharts.set(false);
        },
        error: (err) => {
          this.loadingCharts.set(false);
          this.notify.error(errorMessage(err, 'Failed to load favorite exercise charts'));
        },
      });
  }
}