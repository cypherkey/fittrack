import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { trackedParamLabels } from '../../../core/models/enums';
import { Exercise, ExerciseImage, exerciseImageSrc } from '../../../core/models/exercise';
import { errorMessage } from '../../../core/utils/http-error';

export interface ExerciseDetailDialogData {
  exerciseId: string;
}

@Component({
  selector: 'app-exercise-detail-dialog',
  templateUrl: './exercise-detail-dialog.html',
  standalone: false,
  styleUrl: './exercise-detail-dialog.scss',
})
export class ExerciseDetailDialog implements OnInit {
  private readonly exerciseApi = inject(ExerciseApi);

  readonly exercise = signal<Exercise | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly dialogRef: MatDialogRef<ExerciseDetailDialog>,
    @Inject(MAT_DIALOG_DATA) public data: ExerciseDetailDialogData,
  ) {}

  ngOnInit(): void {
    this.exerciseApi.get(this.data.exerciseId).subscribe({
      next: (ex) => {
        this.exercise.set(ex);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'Failed to load exercise'));
      },
    });
  }

  imageSrc(image: ExerciseImage): string | null {
    return exerciseImageSrc(image);
  }

  parameterLabels(flags: number): string {
    const labels = trackedParamLabels(flags);
    return labels.length ? labels.join(', ') : '—';
  }

  close(): void {
    this.dialogRef.close();
  }
}