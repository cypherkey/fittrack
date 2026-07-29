import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
  signal,
} from '@angular/core';
import { FormArray, FormBuilder, FormGroup } from '@angular/forms';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { Exercise } from '../../../core/models/exercise';
import { RPE_LEVELS } from '../../../core/models/enums';
import { ReorderSetItem } from '../../../core/models/template';
import { renumberSets } from '../../utils/set-form';

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
  @Input() entityId: string | null = null;
  @Output() reorderPersisted = new EventEmitter<ReorderSetItem[]>();

  readonly rpeLevels = RPE_LEVELS;
  readonly exercises = signal<Exercise[]>([]);
  exerciseSearch = '';

  ngOnInit(): void {
    this.loadExercises('');
  }

  loadExercises(q: string): void {
    this.exerciseApi
      .list({ q, customOnly: this.catalogOnly ? false : undefined, size: 100 })
      .subscribe((page) => {
        this.exercises.set(
          this.catalogOnly ? page.content.filter((e) => !e.custom) : page.content,
        );
      });
  }

  onExerciseSearch(value: string): void {
    this.exerciseSearch = value;
    this.loadExercises(value);
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

  onExerciseSelected(index: number, exerciseId: string): void {
    const exercise = this.exercises().find((e) => e.id === exerciseId);
    const group = this.sets.at(index);
    group.patchValue({
      exerciseId,
      exerciseName: exercise?.name ?? '',
    });
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
