import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { AuthService } from '../../../core/auth.service';
import { trackedParamLabels } from '../../../core/models/enums';
import {
  Exercise,
  ExerciseHistoryEntry,
  ExerciseImage,
  exerciseImageSrc,
} from '../../../core/models/exercise';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import { WeightProgressSeries } from '../../../shared/utils/weight-progress-chart';

@Component({
  selector: 'app-exercise-detail-page',
  templateUrl: './exercise-detail-page.html',
  standalone: false,
  styleUrl: './exercise-detail-page.scss',
})
export class ExerciseDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly exerciseApi = inject(ExerciseApi);
  private readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);

  readonly exercise = signal<Exercise | null>(null);
  readonly loading = signal(true);
  readonly selectedTab = signal(0);
  readonly history = signal<ExerciseHistoryEntry[]>([]);
  readonly loadingHistory = signal(false);
  readonly chartSeries = signal<WeightProgressSeries[]>([]);

  private touchStartX = 0;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/exercises']);
      return;
    }
    this.exerciseApi.get(id).subscribe({
      next: (ex) => {
        this.exercise.set(ex);
        this.loading.set(false);
        this.loadHistory(ex);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err, 'Exercise not found'));
        void this.router.navigate(['/exercises']);
      },
    });
  }

  canEdit(): boolean {
    const ex = this.exercise();
    if (!ex) {
      return false;
    }
    if (!ex.custom) {
      return !!this.auth.user()?.admin;
    }
    return ex.addedById === this.auth.user()?.id;
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

  imageSrc(image: ExerciseImage): string | null {
    return exerciseImageSrc(image);
  }

  parameterLabels(flags: number): string {
    const labels = trackedParamLabels(flags);
    return labels.length ? labels.join(', ') : '—';
  }

  edit(): void {
    const ex = this.exercise();
    if (ex && this.canEdit()) {
      void this.router.navigate(['/exercises', ex.id, 'edit']);
    }
  }

  back(): void {
    void this.router.navigate(['/exercises']);
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