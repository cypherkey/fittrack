import { Component, inject, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { fromDatetimeLocalValue, toDatetimeLocalValue } from '../../../shared/utils/set-form';

export interface CloneTemplateDialogData {
  templateName: string;
}

export interface CloneTemplateDialogResult {
  performedAt: string;
  name: string | null;
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
    performedAt: [toDatetimeLocalValue(new Date().toISOString()), Validators.required],
    name: [''],
  });

  constructor(
    private readonly dialogRef: MatDialogRef<CloneTemplateDialog, CloneTemplateDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: CloneTemplateDialogData,
  ) {
    this.form.patchValue({ name: data.templateName });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    const v = this.form.value;
    this.dialogRef.close({
      performedAt: fromDatetimeLocalValue(v.performedAt!),
      name: v.name?.trim() || null,
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
