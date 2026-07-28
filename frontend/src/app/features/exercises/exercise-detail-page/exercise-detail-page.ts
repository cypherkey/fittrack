import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ExerciseApi } from '../../../core/api/exercise-api.service';
import { Exercise } from '../../../core/models/exercise';
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

  exercise: Exercise | null = null;
  loading = true;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/exercises']);
      return;
    }
    this.exerciseApi.get(id).subscribe({
      next: (ex) => {
        this.exercise = ex;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.notify.error(errorMessage(err, 'Exercise not found'));
        void this.router.navigate(['/exercises']);
      },
    });
  }

  edit(): void {
    if (this.exercise?.custom) {
      void this.router.navigate(['/exercises', this.exercise.id, 'edit']);
    }
  }

  back(): void {
    void this.router.navigate(['/exercises']);
  }
}
