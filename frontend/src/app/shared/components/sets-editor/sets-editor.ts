import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormArray, FormBuilder, FormGroup } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { Exercise } from '../../../core/models/exercise';
import { RPE_LEVELS } from '../../../core/models/enums';
import { ReorderSetItem } from '../../../core/models/template';
import { renumberSets } from '../../utils/set-form';

interface ExerciseOption {
  id: string;
  name: string;
}

@Component({
  selector: 'app-sets-editor',
  templateUrl: './sets-editor.html',
  standalone: false,
  styleUrl: './sets-editor.scss',
})
export class SetsEditor implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly exerciseApi = inject(ExerciseApi);

  @Input({ required: true }) sets!: FormArray<FormGroup>;
  @Input() catalogOnly = false;
  @Input() showCompleted = false;
  /** When false (templates), RPE controls are hidden and omitted from new rows. */
  @Input() showRpe = true;
  @Input() entityId: string | null = null;
  @Output() reorderPersisted = new EventEmitter<ReorderSetItem[]>();

  readonly rpeLevels = RPE_LEVELS;
  private readonly searchResults = signal<Exercise[]>([]);
  private readonly selectedOptions = signal<ExerciseOption[]>([]);

  readonly exerciseOptions = computed(() => {
    const byId = new Map<string, ExerciseOption>();
    for (const ex of this.searchResults()) {
      byId.set(ex.id, { id: ex.id, name: ex.name });
    }
    for (const ex of this.selectedOptions()) {
      if (!byId.has(ex.id)) {
        byId.set(ex.id, ex);
      }
    }
    return [...byId.values()].sort((a, b) => a.name.localeCompare(b.name));
  });

  ngOnInit(): void {
    this.loadExercises('');
    const selected: ExerciseOption[] = [];
    for (const group of this.sets.controls) {
      const id = group.get('exerciseId')?.value as string | undefined;
      const name = group.get('exerciseName')?.value as string | undefined;
      if (id && name) {
        selected.push({ id, name });
      }
    }
    if (selected.length) {
      this.selectedOptions.set(selected);
    }
  }

  loadExercises(q: string): void {
    this.exerciseApi
      .list({ q, customOnly: this.catalogOnly ? false : undefined, size: 100 })
      .subscribe((page) => {
        const content = this.catalogOnly
          ? page.content.filter((e) => !e.custom)
          : page.content;
        this.searchResults.set(content);
      });
  }

  onExerciseInput(index: number): void {
    const group = this.sets.at(index);
    const value = (group.get('exerciseName')?.value as string) || '';
    // Require an explicit autocomplete pick for a valid exerciseId.
    group.patchValue({ exerciseId: '' }, { emitEvent: false });
    this.loadExercises(value);
  }

  onExerciseFocus(index: number): void {
    const name = (this.sets.at(index).get('exerciseName')?.value as string) || '';
    this.loadExercises(name);
  }

  onExerciseAutocompleteSelected(index: number, event: MatAutocompleteSelectedEvent): void {
    const name = String(event.option.value ?? '');
    const exercise =
      this.exerciseOptions().find((e) => e.name === name) ??
      this.searchResults().find((e) => e.name === name);
    const group = this.sets.at(index);
    group.patchValue({
      exerciseId: exercise?.id ?? '',
      exerciseName: name,
    });
    if (exercise) {
      const next = this.selectedOptions().filter((e) => e.id !== exercise.id);
      next.push({ id: exercise.id, name: exercise.name });
      this.selectedOptions.set(next);
    }
  }

  addSet(): void {
    const next = this.sets.length + 1;
    const group = this.fb.group({
      id: [null as string | null],
      exerciseId: [''],
      exerciseName: [''],
      setNumber: [next],
      reps: [null as number | null],
      weightKg: [null as number | null],
      durationSeconds: [null as number | null],
      distanceMeters: [null as number | null],
      completed: [true],
      rpe: [null],
      notes: [''],
    });
    if (!this.showCompleted) {
      group.removeControl('completed' as never);
    }
    if (!this.showRpe) {
      group.removeControl('rpe' as never);
    }
    this.sets.push(group);
  }

  removeSet(index: number): void {
    this.sets.removeAt(index);
    renumberSets(this.sets.controls as FormGroup[]);
  }

  moveUp(index: number): void {
    if (index <= 0) {
      return;
    }
    const controls = this.sets.controls as FormGroup[];
    [controls[index - 1], controls[index]] = [controls[index], controls[index - 1]];
    this.sets.clear();
    controls.forEach((c) => this.sets.push(c));
    this.afterReorder();
  }

  moveDown(index: number): void {
    if (index >= this.sets.length - 1) {
      return;
    }
    const controls = this.sets.controls as FormGroup[];
    [controls[index], controls[index + 1]] = [controls[index + 1], controls[index]];
    this.sets.clear();
    controls.forEach((c) => this.sets.push(c));
    this.afterReorder();
  }

  drop(event: CdkDragDrop<FormGroup[]>): void {
    const controls = this.sets.controls as FormGroup[];
    moveItemInArray(controls, event.previousIndex, event.currentIndex);
    this.sets.clear();
    controls.forEach((c) => this.sets.push(c));
    this.afterReorder();
  }

  private afterReorder(): void {
    renumberSets(this.sets.controls as FormGroup[]);
    const allHaveIds = (this.sets.controls as FormGroup[]).every((g) => !!g.get('id')?.value);
    if (this.entityId && allHaveIds) {
      const items: ReorderSetItem[] = (this.sets.controls as FormGroup[]).map((g) => ({
        setId: g.get('id')!.value as string,
        setNumber: g.get('setNumber')!.value as number,
      }));
      this.reorderPersisted.emit(items);
    }
  }
}
