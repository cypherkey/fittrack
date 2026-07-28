import { Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { WORKOUT_DIFFICULTIES } from '../../../core/models/enums';
import { ReorderSetItem } from '../../../core/models/template';
import { WorkoutRequest, WorkoutSetRequest } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import {
  createWorkoutSetGroup,
  fromDatetimeLocalValue,
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
  private readonly notify = inject(NotificationService);

  readonly difficulties = WORKOUT_DIFFICULTIES;

  workoutId: string | null = null;
  loading = false;
  saving = false;

  readonly form = this.fb.group({
    performedAt: [toDatetimeLocalValue(new Date().toISOString()), Validators.required],
    name: [''],
    durationSeconds: [null as number | null],
    difficulty: [''],
    notes: [''],
    sets: this.fb.array([] as ReturnType<typeof createWorkoutSetGroup>[]),
  });

  get sets(): FormArray {
    return this.form.get('sets') as FormArray;
  }

  ngOnInit(): void {
    this.workoutId = this.route.snapshot.paramMap.get('id');
    if (this.workoutId) {
      this.loading = true;
      this.workoutApi.get(this.workoutId).subscribe({
        next: (w) => {
          this.form.patchValue({
            performedAt: toDatetimeLocalValue(w.performedAt),
            name: w.name ?? '',
            durationSeconds: w.durationSeconds,
            difficulty: w.difficulty ?? '',
            notes: w.notes ?? '',
          });
          w.sets.forEach((s) => this.sets.push(createWorkoutSetGroup(this.fb, s)));
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          this.notify.error(errorMessage(err));
          void this.router.navigate(['/workouts']);
        },
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
      performedAt: fromDatetimeLocalValue(v.performedAt!),
      name: v.name || null,
      durationSeconds: v.durationSeconds,
      difficulty: (v.difficulty as WorkoutRequest['difficulty']) || null,
      notes: v.notes || null,
      sets: (this.sets.value as WorkoutSetRequest[]).map((s) => ({
        exerciseId: s.exerciseId,
        setNumber: s.setNumber,
        reps: s.reps,
        weightKg: s.weightKg,
        durationSeconds: s.durationSeconds,
        distanceMeters: s.distanceMeters,
        completed: s.completed ?? true,
        rpe: s.rpe,
        notes: s.notes || null,
      })),
    };

    this.saving = true;
    const req = this.workoutId
      ? this.workoutApi.update(this.workoutId, body)
      : this.workoutApi.create(body);

    req.subscribe({
      next: (w) => {
        this.saving = false;
        this.notify.success(this.workoutId ? 'Workout updated' : 'Workout logged');
        void this.router.navigate(['/workouts', w.id]);
      },
      error: (err) => {
        this.saving = false;
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
