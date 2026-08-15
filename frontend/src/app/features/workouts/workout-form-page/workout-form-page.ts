import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { pairwise, startWith } from 'rxjs';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { AuthService } from '../../../core/auth.service';
import { WORKOUT_DIFFICULTIES } from '../../../core/models/enums';
import { ReorderSetItem } from '../../../core/models/template';
import { WorkoutRequest, WorkoutSet, WorkoutSetRequest } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import {
  createWorkoutSetGroup,
  formatSessionDuration,
  fromDatetimeLocalValueOrNull,
  toDatetimeLocalValue,
} from '../../../shared/utils/set-form';
import { kgToLb, lbToKg, toDisplayWeight, toStorageWeight } from '../../../shared/utils/units';

@Component({
  selector: 'app-workout-form-page',
  templateUrl: './workout-form-page.html',
  standalone: false,
  styleUrl: './workout-form-page.scss',
})
export class WorkoutFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly workoutApi = inject(WorkoutApi);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly difficulties = WORKOUT_DIFFICULTIES;

  workoutId: string | null = null;
  readonly loading = signal(false);
  readonly saving = signal(false);

  readonly form = this.fb.group({
    startedAt: [''],
    endedAt: [''],
    name: [''],
    completed: [false],
    useMetric: [true],
    difficulty: [''],
    notes: [''],
    sets: this.fb.array([] as ReturnType<typeof createWorkoutSetGroup>[]),
  });

  get sets(): FormArray {
    return this.form.get('sets') as FormArray;
  }

  get useMetricPreference(): boolean {
    return this.form.value.useMetric ?? true;
  }

  sessionDurationLabel(): string {
    const v = this.form.value;
    const started = v.startedAt ? new Date(v.startedAt).toISOString() : null;
    const ended = v.endedAt ? new Date(v.endedAt).toISOString() : null;
    return formatSessionDuration(started, ended);
  }

  ngOnInit(): void {
    this.form
      .get('useMetric')!
      .valueChanges.pipe(startWith(this.form.value.useMetric ?? true), pairwise(), takeUntilDestroyed(this.destroyRef))
      .subscribe(([prev, next]) => {
        if (prev === next || prev == null || next == null) {
          return;
        }
        this.convertSetWeightsInForm(!!prev, !!next);
      });

    this.workoutId = this.route.snapshot.paramMap.get('id');
    if (this.workoutId) {
      this.loading.set(true);
      this.workoutApi.get(this.workoutId).subscribe({
        next: (w) => {
          // Display/input units follow Settings; form checkbox defaults to that preference.
          const useMetric = this.auth.user()?.useMetric ?? true;
          this.form.patchValue({
            startedAt: toDatetimeLocalValue(w.startedAt),
            endedAt: toDatetimeLocalValue(w.endedAt),
            name: w.name ?? '',
            completed: w.completed,
            useMetric,
            difficulty: w.difficulty ?? '',
            notes: w.notes ?? '',
          });
          this.replaceSets(w.sets, useMetric);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(errorMessage(err));
          void this.router.navigate(['/workouts']);
        },
      });
    } else {
      this.form.patchValue({
        useMetric: this.auth.user()?.useMetric ?? true,
      });
    }
  }

  onReorder(items: ReorderSetItem[]): void {
    if (!this.workoutId) {
      return;
    }
    this.workoutApi.reorderSets(this.workoutId, { items }).subscribe({
      next: (w) => {
        this.notify.success('Sets reordered');
        this.replaceSets(w.sets, this.form.value.useMetric ?? true);
      },
      error: (err) => this.notify.error(errorMessage(err, 'Failed to reorder sets')),
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const useMetric = v.useMetric ?? true;
    const body: WorkoutRequest = {
      startedAt: fromDatetimeLocalValueOrNull(v.startedAt),
      endedAt: fromDatetimeLocalValueOrNull(v.endedAt),
      name: v.name || null,
      completed: v.completed ?? false,
      useMetric,
      difficulty: (v.difficulty as WorkoutRequest['difficulty']) || null,
      notes: v.notes || null,
      sets: (this.sets.value as WorkoutSetRequest[]).map((s) => ({
        exerciseId: s.exerciseId,
        setNumber: s.setNumber,
        reps: s.reps,
        weightKg: toStorageWeight(s.weightKg, useMetric),
        durationSeconds: s.durationSeconds,
        distanceMeters: s.distanceMeters,
        completed: s.completed ?? false,
        rpe: s.rpe,
      })),
    };

    this.saving.set(true);
    const req = this.workoutId
      ? this.workoutApi.update(this.workoutId, body)
      : this.workoutApi.create(body);

    req.subscribe({
      next: (w) => {
        this.saving.set(false);
        this.notify.success(this.workoutId ? 'Workout updated' : 'Workout logged');
        void this.router.navigate(['/workouts', w.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.error(errorMessage(err, 'Failed to save workout'));
      },
    });
  }

  cancel(): void {
    if (this.workoutId) {
      void this.router.navigate(['/workouts', this.workoutId]);
    } else {
      void this.router.navigate(['/workouts']);
    }
  }

  private replaceSets(sets: WorkoutSet[], useMetric: boolean): void {
    this.sets.clear();
    sets.forEach((s) => {
      const group = createWorkoutSetGroup(this.fb, s);
      group.patchValue(
        { weightKg: toDisplayWeight(s.weightKg, useMetric) },
        { emitEvent: false },
      );
      this.sets.push(group);
    });
  }

  /** When the metric checkbox flips, reinterpret form weight values in the new unit. */
  private convertSetWeightsInForm(wasMetric: boolean, isMetric: boolean): void {
    for (const control of this.sets.controls) {
      const group = control as FormGroup;
      const weightCtrl = group.get('weightKg');
      const value = weightCtrl?.value as number | null;
      if (value == null || weightCtrl == null) {
        continue;
      }
      const next = wasMetric && !isMetric ? kgToLb(value) : !wasMetric && isMetric ? lbToKg(value) : value;
      weightCtrl.setValue(next, { emitEvent: false });
    }
  }
}