import { Component, OnInit, inject, signal } from '@angular/core';
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

  readonly workout = signal<Workout | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/workouts']);
      return;
    }
    this.workoutApi.get(id).subscribe({
      next: (w) => {
        this.workout.set(w);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err));
        void this.router.navigate(['/workouts']);
      },
    });
  }

  edit(): void {
    const w = this.workout();
    if (w) {
      void this.router.navigate(['/workouts', w.id, 'edit']);
    }
  }

  back(): void {
    void this.router.navigate(['/workouts']);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}
