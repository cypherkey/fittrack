import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { AuthService } from '../../../core/auth.service';
import { ExerciseHistoryEntry } from '../../../core/models/exercise';
import { errorMessage } from '../../../core/utils/http-error';
import { formatWeight, weightUnitLabel } from '../../utils/units';

export interface ExerciseHistoryDialogData {
  exerciseId: string;
  exerciseName?: string;
}

@Component({
  selector: 'app-exercise-history-dialog',
  templateUrl: './exercise-history-dialog.html',
  standalone: false,
  styleUrl: './exercise-history-dialog.scss',
})
export class ExerciseHistoryDialog implements OnInit {
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly auth = inject(AuthService);

  readonly rows = signal<ExerciseHistoryEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly columns = ['date', 'setNumber', 'reps', 'weight'] as const;

  constructor(
    private readonly dialogRef: MatDialogRef<ExerciseHistoryDialog>,
    @Inject(MAT_DIALOG_DATA) public data: ExerciseHistoryDialogData,
  ) {}

  ngOnInit(): void {
    this.exerciseApi.history(this.data.exerciseId).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'Failed to load history'));
      },
    });
  }

  title(): string {
    const name = this.data.exerciseName?.trim();
    return name ? name + ' history' : 'Exercise history';
  }

  useMetric(): boolean {
    return this.auth.user()?.useMetric ?? true;
  }

  weightColumnLabel(): string {
    return `Weight (${weightUnitLabel(this.useMetric())})`;
  }

  formatWeightValue(weightKg: number | null | undefined): string {
    return formatWeight(weightKg, this.useMetric(), '-');
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) {
      return '-';
    }
    return new Date(iso).toLocaleString();
  }

  close(): void {
    this.dialogRef.close();
  }
}