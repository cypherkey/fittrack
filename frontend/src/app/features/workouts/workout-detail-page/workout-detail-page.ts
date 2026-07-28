import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkoutApi } from '../../../core/api/workout-api.service';
import { Workout } from '../../../core/models/workout';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-workout-detail-page',
  templateUrl: './workout-detail-page.html',
  standalone: false,
  styleUrl: './workout-detail-page.scss',
})
export class WorkoutDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly workoutApi = inject(WorkoutApi);
  private readonly notify = inject(NotificationService);

  workout: Workout | null = null;
  loading = true;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/workouts']);
      return;
    }
    this.workoutApi.get(id).subscribe({
      next: (w) => {
        this.workout = w;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.notify.error(errorMessage(err));
        void this.router.navigate(['/workouts']);
      },
    });
  }

  edit(): void {
    if (this.workout) {
      void this.router.navigate(['/workouts', this.workout.id, 'edit']);
    }
  }

  back(): void {
    void this.router.navigate(['/workouts']);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}
