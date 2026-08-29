import { Component, DestroyRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder } from '@angular/forms';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { Router } from '@angular/router';
import {
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  switchMap,
} from 'rxjs';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { AuthService } from '../../../core/auth.service';
import { Exercise } from '../../../core/models/exercise';
import { Workout } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { fromDatetimeLocalValue } from '../../../shared/utils/set-form';
import { formatWeight } from '../../../shared/utils/units';

interface ExerciseOption {
  id: string;
  name: string;
}

@Component({
  selector: 'app-workouts-page',
  templateUrl: './workouts-page.html',
  standalone: false,
  styleUrl: './workouts-page.scss',
})
export class WorkoutsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly workoutApi = inject(WorkoutApi);
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly exerciseSearch$ = new Subject<string>();

  @ViewChild(MatAutocompleteTrigger) private exerciseTrigger?: MatAutocompleteTrigger;

  readonly filterForm = this.fb.group({
    from: [''],
    to: [''],
    exerciseName: [''],
  });

  readonly workouts = signal<Workout[]>([]);
  readonly loading = signal(false);
  private readonly searchResults = signal<Exercise[]>([]);
  private readonly selectedOption = signal<ExerciseOption | null>(null);
  private readonly lastQuery = signal('');
  private selectedExerciseId: string | null = null;

  readonly exerciseOptions = computed(() => {
    const results: ExerciseOption[] = this.searchResults().map((ex) => ({
      id: ex.id,
      name: ex.name,
    }));
    const q = this.lastQuery().trim();
    if (q) {
      return [...results].sort((a, b) => a.name.localeCompare(b.name));
    }
    const byId = new Map<string, ExerciseOption>();
    for (const ex of results) {
      byId.set(ex.id, ex);
    }
    const selected = this.selectedOption();
    if (selected && !byId.has(selected.id)) {
      byId.set(selected.id, selected);
    }
    return [...byId.values()].sort((a, b) => a.name.localeCompare(b.name));
  });

  ngOnInit(): void {
    this.exerciseSearch$
      .pipe(
        debounceTime(200),
        distinctUntilChanged(),
        switchMap((q) => {
          this.lastQuery.set(q);
          return this.fetchExercises(q);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((exercises) => {
        this.searchResults.set(exercises);
        queueMicrotask(() => {
          if (this.exerciseTrigger?.panelOpen) {
            this.exerciseTrigger.openPanel();
          }
        });
      });

    this.loadExercises('');
    this.load();
  }

  private fetchExercises(q: string) {
    return this.exerciseApi.list({ q, size: 100 }).pipe(
      map((page) => [...page.content].sort((a, b) => a.name.localeCompare(b.name))),
      catchError(() => of([] as Exercise[])),
    );
  }

  loadExercises(q: string): void {
    this.lastQuery.set(q);
    this.exerciseSearch$.next(q);
  }

  onExerciseFocus(): void {
    const name = (this.filterForm.get('exerciseName')?.value as string) || '';
    this.loadExercises(name);
  }

  onExerciseInput(): void {
    const value = (this.filterForm.get('exerciseName')?.value as string) || '';
    this.selectedExerciseId = null;
    this.selectedOption.set(null);
    this.loadExercises(value);
  }

  onExerciseAutocompleteSelected(event: MatAutocompleteSelectedEvent): void {
    const name = String(event.option.value ?? '');
    const exercise =
      this.exerciseOptions().find((e) => e.name === name) ??
      this.searchResults().find((e) => e.name === name);
    if (!exercise) {
      this.selectedExerciseId = null;
      this.selectedOption.set(null);
      return;
    }
    this.selectedExerciseId = exercise.id;
    this.selectedOption.set({ id: exercise.id, name: exercise.name });
    this.filterForm.patchValue({ exerciseName: exercise.name }, { emitEvent: false });
  }

  load(): void {
    this.loading.set(true);
    const v = this.filterForm.value;
    this.workoutApi
      .list({
        from: v.from ? fromDatetimeLocalValue(v.from) : undefined,
        to: v.to ? fromDatetimeLocalValue(v.to) : undefined,
        exerciseId: this.selectedExerciseId ?? undefined,
      })
      .subscribe({
        next: (items) => {
          this.workouts.set(items);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notify.error(errorMessage(err, 'Failed to load workouts'));
        },
      });
  }

  applyFilters(): void {
    this.load();
  }

  clearFilters(): void {
    this.selectedExerciseId = null;
    this.selectedOption.set(null);
    this.searchResults.set([]);
    this.filterForm.reset({ from: '', to: '', exerciseName: '' });
    this.loadExercises('');
    this.load();
  }

  create(): void {
    void this.router.navigate(['/workouts/new']);
  }

  view(id: string): void {
    void this.router.navigate(['/workouts', id]);
  }

  start(id: string): void {
    this.workoutApi.start(id).subscribe({
      next: () => {
        this.notify.success('Workout started');
        void this.router.navigate(['/workouts', id]);
      },
      error: (err) => this.notify.error(errorMessage(err, 'Failed to start workout')),
    });
  }

  edit(id: string): void {
    void this.router.navigate(['/workouts', id, 'edit']);
  }

  delete(workout: Workout): void {
    const label = workout.name || workout.startedAt || workout.id;
    if (!confirm(`Delete workout "${label}"?`)) {
      return;
    }
    this.workoutApi.delete(workout.id).subscribe({
      next: () => {
        this.notify.success('Workout deleted');
        this.load();
      },
      error: (err) => this.notify.error(errorMessage(err)),
    });
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '—';
    }
    return new Date(iso).toLocaleString();
  }

  formatTotalWeight(workout: Workout): string {
    return formatWeight(workout.totalWeightLifted, this.auth.user()?.useMetric ?? true);
  }
}