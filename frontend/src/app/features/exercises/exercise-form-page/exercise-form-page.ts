import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { LookupApi } from '../../../core/api/lookup-api.service';
import { EXERCISE_LEVELS, EXERCISE_MECHANICS, ExerciseLevel } from '../../../core/models/enums';
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

  exerciseId: string | null = null;
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
    category: [''],
    trackedParameters: [null as number | null],
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
          if (!ex.custom) {
            this.notify.error('Catalog exercises are read-only');
            void this.router.navigate(['/exercises', ex.id]);
            return;
          }
          this.form.patchValue({
            name: ex.name,
            force: ex.force ?? '',
            level: ex.level,
            mechanic: ex.mechanic ?? '',
            equipmentId: ex.equipmentId ?? '',
            instructions: ex.instructions ?? '',
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

  addMuscle(): void {
    this.muscleLinks.push(
      this.fb.group({
        muscleId: ['', Validators.required],
        primary: [false],
      }),
    );
  }

  removeMuscle(index: number): void {
    this.muscleLinks.removeAt(index);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const body: ExerciseRequest = {
      name: v.name!,
      force: v.force || null,
      level: v.level!,
      mechanic: (v.mechanic as ExerciseRequest['mechanic']) || null,
      equipmentId: v.equipmentId || null,
      instructions: v.instructions || null,
      category: v.category || null,
      trackedParameters: v.trackedParameters,
      muscles: this.muscleLinks.value,
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

  cancel(): void {
    if (this.exerciseId) {
      void this.router.navigate(['/exercises', this.exerciseId]);
    } else {
      void this.router.navigate(['/exercises']);
    }
  }
}
