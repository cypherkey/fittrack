import { Component, Inject, inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface SetNotesDialogData {
  exerciseName?: string;
  setNumber: number;
  notes: string | null;
  readOnly?: boolean;
}

export interface SetNotesDialogResult {
  notes: string | null;
}

@Component({
  selector: 'app-set-notes-dialog',
  templateUrl: './set-notes-dialog.html',
  standalone: false,
  styleUrl: './set-notes-dialog.scss',
})
export class SetNotesDialog {
  private readonly fb = inject(FormBuilder);

  readonly readOnly: boolean;
  readonly form = this.fb.group({
    notes: [''],
  });

  constructor(
    private readonly dialogRef: MatDialogRef<SetNotesDialog, SetNotesDialogResult | undefined>,
    @Inject(MAT_DIALOG_DATA) public data: SetNotesDialogData,
  ) {
    this.readOnly = !!data.readOnly;
    this.form.patchValue({ notes: data.notes ?? '' });
    if (this.readOnly) {
      this.form.disable();
    }
  }

  title(): string {
    const name = this.data.exerciseName?.trim();
    const setLabel = `Set ${this.data.setNumber}`;
    return name ? `${name} — ${setLabel} notes` : `${setLabel} notes`;
  }

  save(): void {
    if (this.readOnly) {
      this.dialogRef.close();
      return;
    }
    const raw = (this.form.getRawValue().notes ?? '').trim();
    this.dialogRef.close({ notes: raw || null });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
