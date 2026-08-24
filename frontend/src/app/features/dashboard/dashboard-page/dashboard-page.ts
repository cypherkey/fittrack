import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ChartConfiguration, ChartData } from 'chart.js';
import 'chartjs-adapter-date-fns';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { AuthService } from '../../../core/auth.service';
import { Exercise, ExerciseHistoryEntry } from '../../../core/models/exercise';
import { Workout } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { formatWeight, toDisplayWeight, weightUnitLabel } from '../../../shared/utils/units';

const CHART_COLORS = ['#005cbb', '#006a6a', '#8f4e00', '#ba1a1a', '#006e1c', '#6750a4', '#984061'];

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
  readonly chartData = signal<ChartData<'line'>>({ datasets: [] });
  readonly loadingCharts = signal(false);

  readonly hasChartData = computed(() =>
    this.chartData().datasets.some((dataset) => dataset.data.length > 0),
  );

  readonly chartOptions = computed((): ChartConfiguration<'line'>['options'] => {
    const useMetric = this.auth.user()?.useMetric ?? true;
    return {
      responsive: true,
      maintainAspectRatio: false,
      parsing: false,
      interaction: {
        mode: 'nearest',
        intersect: false,
      },
      scales: {
        x: {
          type: 'time',
          time: {
            unit: 'day',
            tooltipFormat: 'PP',
            displayFormats: {
              day: 'MMM d, yyyy',
            },
          },
          title: {
            display: true,
            text: 'Date',
          },
        },
        y: {
          title: {
            display: true,
            text: `Weight (${weightUnitLabel(useMetric)})`,
          },
          beginAtZero: false,
        },
      },
      plugins: {
        legend: {
          display: true,
          position: 'bottom',
        },
        tooltip: {
          callbacks: {
            label: (context) => {
              const y = context.parsed.y;
              if (y == null) {
                return context.dataset.label ?? '';
              }
              return `${context.dataset.label}: ${y} ${weightUnitLabel(useMetric)}`;
            },
          },
        },
      },
    };
  });

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
          this.chartData.set(this.buildChartData(this.favoriteExercises(), histories));
          this.loadingCharts.set(false);
        },
        error: (err) => {
          this.loadingCharts.set(false);
          this.notify.error(errorMessage(err, 'Failed to load favorite exercise charts'));
        },
      });
  }

  private buildChartData(
    favorites: Exercise[],
    histories: ExerciseHistoryEntry[][],
  ): ChartData<'line'> {
    const useMetric = this.auth.user()?.useMetric ?? true;
    const datasets: ChartData<'line'>['datasets'] = [];

    for (let i = 0; i < favorites.length; i++) {
      const points = this.historyToPoints(histories[i] ?? [], useMetric);
      if (points.length === 0) {
        continue;
      }
      const color = CHART_COLORS[datasets.length % CHART_COLORS.length];
      datasets.push({
        label: favorites[i].name,
        data: points,
        borderColor: color,
        backgroundColor: color,
        tension: 0.2,
        pointRadius: 3,
        pointHoverRadius: 5,
      });
    }

    return { datasets };
  }

  /** Max display weight per workout calendar date for chart points. */
  private historyToPoints(
    history: ExerciseHistoryEntry[],
    useMetric: boolean,
  ): { x: number; y: number }[] {
    const maxByDate = new Map<number, number>();

    for (const entry of history) {
      if (!entry.startedAt || entry.weightKg == null) {
        continue;
      }
      const weight = toDisplayWeight(entry.weightKg, useMetric);
      if (weight == null) {
        continue;
      }
      const dayMs = this.toLocalDateMs(entry.startedAt);
      const prev = maxByDate.get(dayMs);
      if (prev == null || weight > prev) {
        maxByDate.set(dayMs, weight);
      }
    }

    return [...maxByDate.entries()]
      .map(([dayMs, weight]) => ({ x: dayMs, y: weight }))
      .sort((a, b) => a.x - b.x);
  }

  /** Local calendar date (midnight) from an ISO workout startedAt. */
  private toLocalDateMs(iso: string): number {
    const d = new Date(iso);
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  }
}
