import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { NotificationService } from '../../../core/services/notification.service';
import { trackedParamLabels } from '../../../core/models/enums';
import {
  Exercise,
  ExerciseHistoryEntry,
  ExerciseImage,
  exerciseImageSrc,
} from '../../../core/models/exercise';
import { errorMessage } from '../../../core/utils/http-error';
import { WeightProgressSeries } from '../../utils/weight-progress-chart';

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
  private readonly notify = inject(NotificationService);

  readonly exercise = signal<Exercise | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly selectedTab = signal(0);
  readonly history = signal<ExerciseHistoryEntry[]>([]);
  readonly loadingHistory = signal(false);
  readonly chartSeries = signal<WeightProgressSeries[]>([]);

  private touchStartX = 0;

  constructor(
    private readonly dialogRef: MatDialogRef<ExerciseDetailDialog>,
    @Inject(MAT_DIALOG_DATA) public data: ExerciseDetailDialogData,
  ) {}

  ngOnInit(): void {
    this.exerciseApi.get(this.data.exerciseId).subscribe({
      next: (ex) => {
        this.exercise.set(ex);
        this.loading.set(false);
        this.loadHistory(ex);
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

  toggleFavorite(): void {
    const ex = this.exercise();
    if (!ex) {
      return;
    }
    const req = ex.favorite ? this.exerciseApi.unfavorite(ex.id) : this.exerciseApi.favorite(ex.id);
    req.subscribe({
      next: (updated) => this.exercise.set(updated),
      error: (err) => this.notify.error(errorMessage(err, 'Failed to update favorite')),
    });
  }

  onSelectedTabChange(index: number): void {
    this.selectedTab.set(index);
  }

  onTabTouchStart(event: TouchEvent): void {
    this.touchStartX = event.changedTouches[0]?.screenX ?? 0;
  }

  onTabTouchEnd(event: TouchEvent): void {
    const endX = event.changedTouches[0]?.screenX ?? this.touchStartX;
    const dx = endX - this.touchStartX;
    const threshold = 56;
    if (Math.abs(dx) < threshold) {
      return;
    }
    if (dx < 0 && this.selectedTab() < 1) {
      this.selectedTab.set(1);
    } else if (dx > 0 && this.selectedTab() > 0) {
      this.selectedTab.set(0);
    }
  }

  private loadHistory(ex: Exercise): void {
    this.loadingHistory.set(true);
    this.exerciseApi.history(ex.id).subscribe({
      next: (rows) => {
        this.history.set(rows);
        this.chartSeries.set([{ label: ex.name, history: rows }]);
        this.loadingHistory.set(false);
      },
      error: (err) => {
        this.loadingHistory.set(false);
        this.notify.error(errorMessage(err, 'Failed to load exercise progress'));
      },
    });
  }
}