import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { AuthService } from '../../../core/auth.service';
import { RPE_LEVELS, RpeLevel, TRACKED_PARAM, hasTrackedParam } from '../../../core/models/enums';
import { Workout, WorkoutSet, WorkoutSetPatchRequest } from '../../../core/models/workout';
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
import {
  SetNotesDialog,
  SetNotesDialogData,
  SetNotesDialogResult,
} from '../../../shared/components/set-notes-dialog/set-notes-dialog';
import { formatSessionDuration } from '../../../shared/utils/set-form';
import {
  formatWeight,
  toDisplayWeight,
  toStorageWeight,
  weightUnitLabel,
} from '../../../shared/utils/units';

export type TrackedMetricKey = 'reps' | 'weightKg' | 'durationSeconds' | 'distanceMeters';

export interface TrackedMetricField {
  key: TrackedMetricKey;
  label: string;
  step: string | null;
}

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
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly workout = signal<Workout | null>(null);
  readonly loading = signal(true);
  readonly acting = signal(false);
  /** Set ids with an in-flight PATCH (does not block further edits — those coalesce). */
  readonly setActingIds = signal<ReadonlySet<string>>(new Set());
  readonly setColumns = ['setNumber', 'exercise', 'metrics', 'rpe', 'completed'] as const;
  readonly rpeLevels = RPE_LEVELS;

  /** Latest unsent field merges per set while a PATCH is in flight. */
  private readonly pendingPatches = new Map<string, WorkoutSetPatchRequest>();

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

  editNotes(row: WorkoutSet): void {
    const ref = this.dialog.open<
      SetNotesDialog,
      SetNotesDialogData,
      SetNotesDialogResult | undefined
    >(SetNotesDialog, {
      data: {
        exerciseName: row.exerciseName,
        notes: row.exerciseNotes,
      },
      maxWidth: '520px',
      width: '92vw',
    });
    ref.afterClosed().subscribe((result) => {
      if (!result) {
        return;
      }
      const next = result.notes;
      const current = row.exerciseNotes;
      if (current === next || (!current && !next)) {
        return;
      }
      this.exerciseApi.putNotes(row.exerciseId, next).subscribe({
        next: (saved) => {
          const w = this.workout();
          if (!w) {
            return;
          }
          this.workout.set({
            ...w,
            sets: w.sets.map((s) =>
              s.exerciseId === row.exerciseId ? { ...s, exerciseNotes: saved.notes } : s,
            ),
          });
        },
        error: (err) => this.notify.error(errorMessage(err, 'Failed to save notes')),
      });
    });
  }

  isSetActing(setId: string): boolean {
    return this.setActingIds().has(setId);
  }

  setsReadOnly(): boolean {
    return this.workout()?.completed === true;
  }

  isSetDisabled(_setId: string): boolean {
    // Keep controls editable while a PATCH is in flight so rapid changes can coalesce.
    return this.setsReadOnly();
  }

  /** Display/input units follow the signed-in user preference from Settings. */
  useMetricPreference(): boolean {
    return this.auth.user()?.useMetric ?? true;
  }

  metricFields(row: WorkoutSet): TrackedMetricField[] {
    const flags = row.trackedParameters ?? 0;
    const useMetric = this.useMetricPreference();
    const fields: TrackedMetricField[] = [];
    if (hasTrackedParam(flags, TRACKED_PARAM.REPS)) {
      fields.push({ key: 'reps', label: 'Reps', step: null });
    }
    if (hasTrackedParam(flags, TRACKED_PARAM.WEIGHT)) {
      fields.push({ key: 'weightKg', label: weightUnitLabel(useMetric), step: '0.5' });
    }
    if (hasTrackedParam(flags, TRACKED_PARAM.DURATION)) {
      fields.push({ key: 'durationSeconds', label: 'sec', step: null });
    }
    if (hasTrackedParam(flags, TRACKED_PARAM.DISTANCE)) {
      // Distance is always meters regardless of metric preference.
      fields.push({
        key: 'distanceMeters',
        label: 'm',
        step: '0.1',
      });
    }
    return fields.slice(0, 2);
  }

  metricValue(row: WorkoutSet, key: TrackedMetricKey): number | null {
    if (key === 'weightKg') {
      return toDisplayWeight(row.weightKg, this.useMetricPreference());
    }
    return row[key];
  }

  totalWeightLabel(workout: Workout): string {
    return formatWeight(workout.totalWeightLifted, this.useMetricPreference(), '-');
  }

  rpeShort(level: RpeLevel): string {
    switch (level) {
      case RpeLevel.Easy:
        return 'Easy';
      case RpeLevel.Challenging:
        return 'Chall.';
      case RpeLevel.Hard:
        return 'Hard';
      default:
        return level;
    }
  }

  toggleSetCompleted(row: WorkoutSet, completed: boolean): void {
    if (row.completed === completed) {
      return;
    }
    this.patchSet(row, { completed });
  }

  onRpeChipToggle(row: WorkoutSet, level: RpeLevel): void {
    this.onRpeChange(row, row.rpe === level ? null : level);
  }

  onRpeChange(row: WorkoutSet, rpe: RpeLevel | null): void {
    if (row.rpe === rpe) {
      return;
    }
    this.patchSet(row, { rpe });
  }

  onMetricChange(row: WorkoutSet, key: TrackedMetricKey, raw: string): void {
    const parsed = raw.trim() === '' ? null : Number(raw);
    if (parsed !== null && Number.isNaN(parsed)) {
      return;
    }
    const currentDisplay = this.metricValue(row, key);
    if (currentDisplay === parsed || (currentDisplay == null && parsed == null)) {
      return;
    }
    const storageValue =
      key === 'weightKg' ? toStorageWeight(parsed, this.useMetricPreference()) : parsed;
    this.patchSet(row, { [key]: storageValue });
  }

  /**
   * Optimistic local apply + coalesce: one in-flight PATCH per set; further edits merge
   * into a pending body and flush when the current request completes.
   */
  private patchSet(row: WorkoutSet, body: WorkoutSetPatchRequest): void {
    const w = this.workout();
    if (!w || w.completed) {
      return;
    }

    this.applyLocalPatch(row.id, body);

    if (this.isSetActing(row.id)) {
      const pending = this.pendingPatches.get(row.id) ?? {};
      this.pendingPatches.set(row.id, { ...pending, ...body });
      return;
    }

    this.sendSetPatch(w.id, row.id, body);
  }

  private sendSetPatch(workoutId: string, setId: string, body: WorkoutSetPatchRequest): void {
    this.markSetActing(setId, true);

    this.workoutApi.patchSet(workoutId, setId, body).subscribe({
      next: (updated) => {
        const pending = this.pendingPatches.get(setId);
        this.pendingPatches.delete(setId);
        this.markSetActing(setId, false);

        if (pending && Object.keys(pending).length > 0) {
          this.workout.set(updated);
          this.applyLocalPatch(setId, pending);
          this.sendSetPatch(workoutId, setId, pending);
          return;
        }

        this.workout.set(updated);
      },
      error: (err) => {
        this.pendingPatches.delete(setId);
        this.markSetActing(setId, false);
        this.notify.error(errorMessage(err, 'Failed to update set'));
        this.workoutApi.get(workoutId).subscribe({
          next: (fresh) => this.workout.set(fresh),
        });
      },
    });
  }

  private applyLocalPatch(setId: string, body: WorkoutSetPatchRequest): void {
    const w = this.workout();
    if (!w) {
      return;
    }
    this.workout.set({
      ...w,
      sets: w.sets.map((s) => (s.id === setId ? { ...s, ...body } : s)),
    });
  }

  private markSetActing(setId: string, acting: boolean): void {
    const next = new Set(this.setActingIds());
    if (acting) {
      next.add(setId);
    } else {
      next.delete(setId);
    }
    this.setActingIds.set(next);
  }
}