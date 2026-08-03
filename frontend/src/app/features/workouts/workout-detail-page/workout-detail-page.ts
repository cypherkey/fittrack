import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { Workout } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import {
  ExerciseDetailDialog,
  ExerciseDetailDialogData,
} from '../../../shared/components/exercise-detail-dialog/exercise-detail-dialog';
import {
  ExerciseHistoryDialog,
  ExerciseHistoryDialogData,
} from '../../../shared/components/exercise-history-dialog/exercise-history-dialog';
import { formatSessionDuration } from '../../../shared/utils/set-form';

@Component({
  selector: 'app-workout-detail-page',
  templateUrl: './workout-detail-page.html',
  standalone: false,
  styleUrl: './workout-detail-page.scss',
})
export class WorkoutDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly workoutApi = inject(WorkoutApi);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly workout = signal<Workout | null>(null);
  readonly loading = signal(true);
  readonly acting = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/workouts']);
      return;
    }
    this.workoutApi.get(id).subscribe({
      next: (w) => {
        this.workout.set(w);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err));
        void this.router.navigate(['/workouts']);
      },
    });
  }

  start(): void {
    const w = this.workout();
    if (!w || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.workoutApi.start(w.id).subscribe({
      next: (updated) => {
        this.workout.set(updated);
        this.acting.set(false);
        this.notify.success('Workout started');
      },
      error: (err) => {
        this.acting.set(false);
        this.notify.error(errorMessage(err));
      },
    });
  }

  complete(): void {
    const w = this.workout();
    if (!w || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.workoutApi.complete(w.id).subscribe({
      next: (updated) => {
        this.workout.set(updated);
        this.acting.set(false);
        this.notify.success('Workout completed');
      },
      error: (err) => {
        this.acting.set(false);
        this.notify.error(errorMessage(err));
      },
    });
  }

  edit(): void {
    const w = this.workout();
    if (w) {
      void this.router.navigate(['/workouts', w.id, 'edit']);
    }
  }

  back(): void {
    void this.router.navigate(['/workouts']);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '-';
    }
    return new Date(iso).toLocaleString();
  }

  durationLabel(workout: Workout): string {
    return formatSessionDuration(workout.startedAt, workout.endedAt);
  }

  showExercise(exerciseId: string): void {
    this.dialog.open<ExerciseDetailDialog, ExerciseDetailDialogData>(ExerciseDetailDialog, {
      data: { exerciseId },
      maxWidth: '640px',
      width: '92vw',
    });
  }

  showHistory(exerciseId: string, exerciseName?: string): void {
    this.dialog.open<ExerciseHistoryDialog, ExerciseHistoryDialogData>(ExerciseHistoryDialog, {
      data: { exerciseId, exerciseName },
      maxWidth: '720px',
      width: '92vw',
    });
  }
}