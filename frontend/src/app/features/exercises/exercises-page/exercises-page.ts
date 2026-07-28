import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { LookupApi } from '../../../core/api/lookup-api.service';
import { Exercise } from '../../../core/models/exercise';
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

  displayedColumns = ['name', 'category', 'equipment', 'level', 'custom', 'actions'];
  exercises: Exercise[] = [];
  muscles: Muscle[] = [];
  equipment: Equipment[] = [];
  loading = false;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;

  ngOnInit(): void {
    this.lookupApi.muscles().subscribe((m) => (this.muscles = m));
    this.lookupApi.equipment().subscribe((e) => (this.equipment = e));
    this.load();
  }

  load(): void {
    this.loading = true;
    const v = this.filterForm.value;
    this.exerciseApi
      .list({
        q: v.q || undefined,
        muscle: v.muscle || undefined,
        equipment: v.equipment || undefined,
        category: v.category || undefined,
        customOnly: v.customOnly ?? false,
        page: this.pageIndex,
        size: this.pageSize,
      })
      .subscribe({
        next: (page) => {
          this.exercises = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          this.notify.error(errorMessage(err, 'Failed to load exercises'));
        },
      });
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.load();
  }

  clearFilters(): void {
    this.filterForm.reset({ q: '', muscle: '', equipment: '', category: '', customOnly: false });
    this.pageIndex = 0;
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
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
