import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { LookupApi } from '../../../core/api/lookup-api.service';
import {
  DEFAULT_TRACKED_PARAMETERS,
  EXERCISE_CATEGORIES,
  EXERCISE_FORCES,
  EXERCISE_LEVELS,
  EXERCISE_MECHANICS,
  ExerciseLevel,
  TRACKED_PARAM_OPTIONS,
  hasTrackedParam,
  toggleTrackedParam,
} from '../../../core/models/enums';
import { ExerciseRequest } from '../../../core/models/exercise';
import { Equipment, Muscle } from '../../../core/models/lookup';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-exercise-form-page',
  templateUrl: './exercise-form-page.html',
  standalone: false,
  styleUrl: './exercise-form-page.scss',
})
export class ExerciseFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly lookupApi = inject(LookupApi);
  private readonly notify = inject(NotificationService);

  readonly levels = EXERCISE_LEVELS;
  readonly mechanics = EXERCISE_MECHANICS;
  readonly forces = EXERCISE_FORCES;
  readonly categories = EXERCISE_CATEGORIES;
  readonly trackedParamOptions = TRACKED_PARAM_OPTIONS;

  exerciseId: string | null = null;
  readonly catalogOnly = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly muscles = signal<Muscle[]>([]);
  readonly equipment = signal<Equipment[]>([]);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    force: [''],
    level: [ExerciseLevel.Beginner, Validators.required],
    mechanic: [''],
    equipmentId: [''],
    instructions: [''],
    videoUrl: [''],
    category: [''],
    trackedParameters: [DEFAULT_TRACKED_PARAMETERS as number],
    muscles: this.fb.array([]),
  });

  get muscleLinks(): FormArray {
    return this.form.get('muscles') as FormArray;
  }

  ngOnInit(): void {
    this.lookupApi.muscles().subscribe((m) => this.muscles.set(m));
    this.lookupApi.equipment().subscribe((e) => this.equipment.set(e));

    this.exerciseId = this.route.snapshot.paramMap.get('id');
    if (this.exerciseId) {
      this.loading.set(true);
      this.exerciseApi.get(this.exerciseId).subscribe({
        next: (ex) => {
          this.catalogOnly.set(!ex.custom);
          this.form.patchValue({
            name: ex.name,
            force: ex.force ?? '',
            level: ex.level,
            mechanic: ex.mechanic ?? '',
            equipmentId: ex.equipmentId ?? '',
            instructions: ex.instructions ?? '',
            videoUrl: ex.videoUrl ?? '',
            category: ex.category ?? '',
            trackedParameters: ex.trackedParameters,
          });
          ex.muscles.forEach((m) => {
            this.muscleLinks.push(
              this.fb.group({
                muscleId: [m.muscleId, Validators.required],
                primary: [m.primary],
              }),
            );
          });
          if (!ex.custom) {
            this.lockCatalogFields();
          }
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(errorMessage(err));
          void this.router.navigate(['/exercises']);
        },
      });
    }
  }

  private lockCatalogFields(): void {
    this.form.get('name')?.disable({ emitEvent: false });
    this.form.get('force')?.disable({ emitEvent: false });
    this.form.get('level')?.disable({ emitEvent: false });
    this.form.get('mechanic')?.disable({ emitEvent: false });
    this.form.get('equipmentId')?.disable({ emitEvent: false });
    this.form.get('instructions')?.disable({ emitEvent: false });
    this.form.get('videoUrl')?.disable({ emitEvent: false });
    this.form.get('category')?.disable({ emitEvent: false });
    this.muscleLinks.disable({ emitEvent: false });
  }

  addMuscle(): void {
    if (this.catalogOnly()) {
      return;
    }
    this.muscleLinks.push(
      this.fb.group({
        muscleId: ['', Validators.required],
        primary: [false],
      }),
    );
  }

  removeMuscle(index: number): void {
    if (this.catalogOnly()) {
      return;
    }
    this.muscleLinks.removeAt(index);
  }

  isTrackedParamOn(flag: number): boolean {
    return hasTrackedParam(this.form.get('trackedParameters')?.value, flag);
  }

  onTrackedParamChange(flag: number, checked: boolean): void {
    const next = toggleTrackedParam(this.form.get('trackedParameters')?.value, flag, checked);
    this.form.patchValue({ trackedParameters: next });
  }

  save(): void {
    if (this.catalogOnly()) {
      this.saveTrackedParametersOnly();
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: ExerciseRequest = {
      name: v.name!,
      force: v.force || null,
      level: v.level!,
      mechanic: (v.mechanic as ExerciseRequest['mechanic']) || null,
      equipmentId: v.equipmentId || null,
      instructions: v.instructions || null,
      videoUrl: v.videoUrl?.trim() || null,
      category: v.category || null,
      trackedParameters: v.trackedParameters ?? DEFAULT_TRACKED_PARAMETERS,
      muscles: this.muscleLinks.getRawValue(),
    };

    this.saving.set(true);
    const req = this.exerciseId
      ? this.exerciseApi.update(this.exerciseId, body)
      : this.exerciseApi.create(body);

    req.subscribe({
      next: (ex) => {
        this.saving.set(false);
        this.notify.success(this.exerciseId ? 'Exercise updated' : 'Exercise created');
        void this.router.navigate(['/exercises', ex.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.error(errorMessage(err, 'Failed to save exercise'));
      },
    });
  }

  private saveTrackedParametersOnly(): void {
    if (!this.exerciseId) {
      return;
    }
    const trackedParameters = this.form.get('trackedParameters')?.value ?? DEFAULT_TRACKED_PARAMETERS;
    this.saving.set(true);
    this.exerciseApi.updateTrackedParameters(this.exerciseId, trackedParameters).subscribe({
      next: (ex) => {
        this.saving.set(false);
        this.notify.success('Tracked parameters updated');
        void this.router.navigate(['/exercises', ex.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.error(errorMessage(err, 'Failed to save tracked parameters'));
      },
    });
  }

  cancel(): void {
    if (this.exerciseId) {
      void this.router.navigate(['/exercises', this.exerciseId]);
    } else {
      void this.router.navigate(['/exercises']);
    }
  }
}
