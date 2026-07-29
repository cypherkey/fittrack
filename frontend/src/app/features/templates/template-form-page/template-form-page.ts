import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TemplateApi } from '../../../core/api/template-api.service';
import {
  TEMPLATE_VISIBILITIES,
  TemplateVisibility,
  WORKOUT_DIFFICULTIES,
} from '../../../core/models/enums';
import { ReorderSetItem, TemplateRequest, TemplateSetRequest } from '../../../core/models/template';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { createTemplateSetGroup } from '../../../shared/utils/set-form';

@Component({
  selector: 'app-template-form-page',
  templateUrl: './template-form-page.html',
  standalone: false,
  styleUrl: './template-form-page.scss',
})
export class TemplateFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly templateApi = inject(TemplateApi);
  private readonly notify = inject(NotificationService);

  readonly visibilities = TEMPLATE_VISIBILITIES;
  readonly difficulties = WORKOUT_DIFFICULTIES;

  templateId: string | null = null;
  readonly loading = signal(false);
  readonly saving = signal(false);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    durationSeconds: [null as number | null],
    difficulty: [''],
    notes: [''],
    visibility: [TemplateVisibility.Private, Validators.required],
    sets: this.fb.array([] as ReturnType<typeof createTemplateSetGroup>[]),
  });

  get sets(): FormArray {
    return this.form.get('sets') as FormArray;
  }

  get catalogOnly(): boolean {
    return this.form.value.visibility === TemplateVisibility.Public;
  }

  ngOnInit(): void {
    this.templateId = this.route.snapshot.paramMap.get('id');
    if (this.templateId) {
      this.loading.set(true);
      this.templateApi.get(this.templateId).subscribe({
        next: (t) => {
          this.form.patchValue({
            name: t.name,
            durationSeconds: t.durationSeconds,
            difficulty: t.difficulty ?? '',
            notes: t.notes ?? '',
            visibility: t.visibility,
          });
          t.sets.forEach((s) => this.sets.push(createTemplateSetGroup(this.fb, s)));
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(errorMessage(err));
          void this.router.navigate(['/templates']);
        },
      });
    }
  }

  onReorder(items: ReorderSetItem[]): void {
    if (!this.templateId) {
      return;
    }
    this.templateApi.reorderSets(this.templateId, { items }).subscribe({
      next: (t) => {
        this.notify.success('Sets reordered');
        this.sets.clear();
        t.sets.forEach((s) => this.sets.push(createTemplateSetGroup(this.fb, s)));
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
    const body: TemplateRequest = {
      name: v.name,
      durationSeconds: v.durationSeconds,
      difficulty: (v.difficulty as TemplateRequest['difficulty']) || null,
      notes: v.notes || null,
      visibility: v.visibility!,
      sets: (this.sets.value as TemplateSetRequest[]).map((s) => ({
        exerciseId: s.exerciseId,
        setNumber: s.setNumber,
        reps: s.reps,
        weightKg: s.weightKg,
        durationSeconds: s.durationSeconds,
        distanceMeters: s.distanceMeters,
        rpe: s.rpe,
        notes: s.notes || null,
      })),
    };

    this.saving.set(true);
    const req = this.templateId
      ? this.templateApi.update(this.templateId, body)
      : this.templateApi.create(body);

    req.subscribe({
      next: (t) => {
        this.saving.set(false);
        this.notify.success(this.templateId ? 'Template updated' : 'Template created');
        void this.router.navigate(['/templates', t.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.notify.error(errorMessage(err, 'Failed to save template'));
      },
    });
  }

  cancel(): void {
    if (this.templateId) {
      void this.router.navigate(['/templates', this.templateId]);
    } else {
      void this.router.navigate(['/templates']);
    }
  }
}
