import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { Exercise, ExerciseImage, exerciseImageSrc } from '../../../core/models/exercise';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';

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
  private readonly notify = inject(NotificationService);

  readonly exercise = signal<Exercise | null>(null);
  readonly loading = signal(true);

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
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err, 'Exercise not found'));
        void this.router.navigate(['/exercises']);
      },
    });
  }

  imageSrc(image: ExerciseImage): string | null {
    return exerciseImageSrc(image);
  }

  edit(): void {
    const ex = this.exercise();
    if (ex?.custom) {
      void this.router.navigate(['/exercises', ex.id, 'edit']);
    }
  }

  back(): void {
    void this.router.navigate(['/exercises']);
  }
}
