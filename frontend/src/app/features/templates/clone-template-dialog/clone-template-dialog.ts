import { Component, inject, Inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface CloneTemplateDialogData {
  templateName: string;
}

export interface CloneTemplateDialogResult {
  name: string | null;
}

/** Local calendar date as Workout YYYY-MM-DD. */
export function defaultCloneWorkoutName(date = new Date()): string {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `Workout ${yyyy}-${mm}-${dd}`;
}

@Component({
  selector: 'app-clone-template-dialog',
  templateUrl: './clone-template-dialog.html',
  standalone: false,
  styleUrl: './clone-template-dialog.scss',
})
export class CloneTemplateDialog {
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    name: [''],
  });

  constructor(
    private readonly dialogRef: MatDialogRef<CloneTemplateDialog, CloneTemplateDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: CloneTemplateDialogData,
  ) {
    this.form.patchValue({ name: defaultCloneWorkoutName() });
  }

  submit(): void {
    const v = this.form.value;
    this.dialogRef.close({
      name: v.name?.trim() || null,
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}