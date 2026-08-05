import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { AuthService } from '../../../core/auth.service';
import { WORKOUT_DIFFICULTIES } from '../../../core/models/enums';
import { ReorderSetItem } from '../../../core/models/template';
import { WorkoutRequest, WorkoutSetRequest } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import {
  createWorkoutSetGroup,
  formatSessionDuration,
  fromDatetimeLocalValueOrNull,
  toDatetimeLocalValue,
} from '../../../shared/utils/set-form';

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

  sessionDurationLabel(): string {
    const v = this.form.value;
    const started = v.startedAt ? new Date(v.startedAt).toISOString() : null;
    const ended = v.endedAt ? new Date(v.endedAt).toISOString() : null;
    return formatSessionDuration(started, ended);
  }

  ngOnInit(): void {
    this.workoutId = this.route.snapshot.paramMap.get('id');
    if (this.workoutId) {
      this.loading.set(true);
      this.workoutApi.get(this.workoutId).subscribe({
        next: (w) => {
          this.form.patchValue({
            startedAt: toDatetimeLocalValue(w.startedAt),
            endedAt: toDatetimeLocalValue(w.endedAt),
            name: w.name ?? '',
            completed: w.completed,
            useMetric: w.useMetric ?? true,
            difficulty: w.difficulty ?? '',
            notes: w.notes ?? '',
          });
          w.sets.forEach((s) => this.sets.push(createWorkoutSetGroup(this.fb, s)));
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
        this.sets.clear();
        w.sets.forEach((s) => this.sets.push(createWorkoutSetGroup(this.fb, s)));
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
    const body: WorkoutRequest = {
      startedAt: fromDatetimeLocalValueOrNull(v.startedAt),
      endedAt: fromDatetimeLocalValueOrNull(v.endedAt),
      name: v.name || null,
      completed: v.completed ?? false,
      useMetric: v.useMetric ?? true,
      difficulty: (v.difficulty as WorkoutRequest['difficulty']) || null,
      notes: v.notes || null,
      sets: (this.sets.value as WorkoutSetRequest[]).map((s) => ({
        exerciseId: s.exerciseId,
        setNumber: s.setNumber,
        reps: s.reps,
        weightKg: s.weightKg,
        durationSeconds: s.durationSeconds,
        distanceMeters: s.distanceMeters,
        completed: s.completed ?? false,
        rpe: s.rpe,
        notes: s.notes || null,
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
}
