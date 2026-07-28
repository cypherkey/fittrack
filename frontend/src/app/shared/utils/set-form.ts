import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RpeLevel } from '../../core/models/enums';
import { TemplateSet } from '../../core/models/template';
import { WorkoutSet } from '../../core/models/workout';

export function createTemplateSetGroup(fb: FormBuilder, set?: TemplateSet): FormGroup {
  return fb.group({
    id: [set?.id ?? null],
    exerciseId: [set?.exerciseId ?? '', Validators.required],
    exerciseName: [set?.exerciseName ?? ''],
    setNumber: [set?.setNumber ?? 1, [Validators.required, Validators.min(1)]],
    reps: [set?.reps ?? null],
    weightKg: [set?.weightKg ?? null],
    durationSeconds: [set?.durationSeconds ?? null],
    distanceMeters: [set?.distanceMeters ?? null],
    rpe: [set?.rpe ?? null as RpeLevel | null],
    notes: [set?.notes ?? ''],
  });
}

export function createWorkoutSetGroup(fb: FormBuilder, set?: WorkoutSet): FormGroup {
  return fb.group({
    id: [set?.id ?? null],
    exerciseId: [set?.exerciseId ?? '', Validators.required],
    exerciseName: [set?.exerciseName ?? ''],
    setNumber: [set?.setNumber ?? 1, [Validators.required, Validators.min(1)]],
    reps: [set?.reps ?? null],
    weightKg: [set?.weightKg ?? null],
    durationSeconds: [set?.durationSeconds ?? null],
    distanceMeters: [set?.distanceMeters ?? null],
    completed: [set?.completed ?? true],
    rpe: [set?.rpe ?? null as RpeLevel | null],
    notes: [set?.notes ?? ''],
  });
}

export function renumberSets(controls: FormGroup[]): void {
  controls.forEach((group, index) => {
    group.get('setNumber')?.setValue(index + 1, { emitEvent: false });
  });
}

export function toInstantIso(date: Date): string {
  return date.toISOString();
}

export function toDatetimeLocalValue(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function fromDatetimeLocalValue(value: string): string {
  return new Date(value).toISOString();
}
