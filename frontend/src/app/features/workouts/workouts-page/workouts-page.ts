import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { AuthService } from '../../../core/auth.service';
import { Workout } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { fromDatetimeLocalValue } from '../../../shared/utils/set-form';
import { formatWeight } from '../../../shared/utils/units';

@Component({
  selector: 'app-workouts-page',
  templateUrl: './workouts-page.html',
  standalone: false,
  styleUrl: './workouts-page.scss',
})
export class WorkoutsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly workoutApi = inject(WorkoutApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  readonly filterForm = this.fb.group({
    from: [''],
    to: [''],
  });

  readonly workouts = signal<Workout[]>([]);
  readonly loading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    this.workoutApi
      .list({
        from: v.from ? fromDatetimeLocalValue(v.from) : undefined,
        to: v.to ? fromDatetimeLocalValue(v.to) : undefined,
      })
      .subscribe({
        next: (items) => {
          this.workouts.set(items);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(errorMessage(err, 'Failed to load workouts'));
        },
      });
  }

  applyFilters(): void {
    this.load();
  }

  clearFilters(): void {
    this.filterForm.reset({ from: '', to: '' });
    this.load();
  }

  create(): void {
    void this.router.navigate(['/workouts/new']);
  }

  view(id: string): void {
    void this.router.navigate(['/workouts', id]);
  }

  start(id: string): void {
    this.workoutApi.start(id).subscribe({
      next: () => {
        this.notify.success('Workout started');
        void this.router.navigate(['/workouts', id]);
      },
      error: (err) => this.notify.error(errorMessage(err, 'Failed to start workout')),
    });
  }

  edit(id: string): void {
    void this.router.navigate(['/workouts', id, 'edit']);
  }

  delete(workout: Workout): void {
    const label = workout.name || workout.startedAt || workout.id;
    if (!confirm(`Delete workout "${label}"?`)) {
      return;
    }
    this.workoutApi.delete(workout.id).subscribe({
      next: () => {
        this.notify.success('Workout deleted');
        this.load();
      },
      error: (err) => this.notify.error(errorMessage(err)),
    });
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
