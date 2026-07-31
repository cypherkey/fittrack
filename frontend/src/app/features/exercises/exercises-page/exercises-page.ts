import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { LookupApi } from '../../../core/api/lookup-api.service';
import { Exercise, exerciseImageSrc } from '../../../core/models/exercise';
import { Equipment, Muscle } from '../../../core/models/lookup';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-exercises-page',
  templateUrl: './exercises-page.html',
  standalone: false,
  styleUrl: './exercises-page.scss',
})
export class ExercisesPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly lookupApi = inject(LookupApi);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  readonly filterForm = this.fb.group({
    q: [''],
    muscle: [''],
    equipment: [''],
    category: [''],
    customOnly: [false],
  });

  displayedColumns = ['image', 'name', 'category', 'equipment', 'level', 'custom', 'actions'];
  readonly exercises = signal<Exercise[]>([]);
  readonly muscles = signal<Muscle[]>([]);
  readonly equipment = signal<Equipment[]>([]);
  readonly loading = signal(false);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);

  thumbSrc(row: Exercise): string | null {
    return exerciseImageSrc(row.images?.[0]);
  }

  ngOnInit(): void {
    this.lookupApi.muscles().subscribe((m) => this.muscles.set(m));
    this.lookupApi.equipment().subscribe((e) => this.equipment.set(e));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    this.exerciseApi
      .list({
        q: v.q || undefined,
        muscle: v.muscle || undefined,
        equipment: v.equipment || undefined,
        category: v.category || undefined,
        customOnly: v.customOnly ?? false,
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .subscribe({
        next: (page) => {
          this.exercises.set(page.content);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(errorMessage(err, 'Failed to load exercises'));
        },
      });
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  clearFilters(): void {
    this.filterForm.reset({ q: '', muscle: '', equipment: '', category: '', customOnly: false });
    this.pageIndex.set(0);
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  view(id: string): void {
    void this.router.navigate(['/exercises', id]);
  }

  createCustom(): void {
    void this.router.navigate(['/exercises/new']);
  }

  edit(id: string): void {
    void this.router.navigate(['/exercises', id, 'edit']);
  }

  delete(exercise: Exercise): void {
    if (!exercise.custom || !confirm(`Delete custom exercise "${exercise.name}"?`)) {
      return;
    }
    this.exerciseApi.delete(exercise.id).subscribe({
      next: () => {
        this.notify.success('Exercise deleted');
        this.load();
      },
      error: (err) => this.notify.error(errorMessage(err, 'Failed to delete exercise')),
    });
  }
}
