import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { TemplateApi } from '../../../core/api/template-api.service';
import { Template } from '../../../core/models/template';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import {
  CloneTemplateDialog,
  CloneTemplateDialogData,
} from '../clone-template-dialog/clone-template-dialog';

@Component({
  selector: 'app-template-detail-page',
  templateUrl: './template-detail-page.html',
  standalone: false,
  styleUrl: './template-detail-page.scss',
})
export class TemplateDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly templateApi = inject(TemplateApi);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly template = signal<Template | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/templates']);
      return;
    }
    this.templateApi.get(id).subscribe({
      next: (t) => {
        this.template.set(t);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err));
        void this.router.navigate(['/templates']);
      },
    });
  }

  edit(): void {
    const t = this.template();
    if (t) {
      void this.router.navigate(['/templates', t.id, 'edit']);
    }
  }

  clone(): void {
    const t = this.template();
    if (!t) {
      return;
    }
    const ref = this.dialog.open<CloneTemplateDialog, CloneTemplateDialogData>(
      CloneTemplateDialog,
      { data: { templateName: t.name }, width: '400px' },
    );
    ref.afterClosed().subscribe((result) => {
      if (!result || !this.template()) {
        return;
      }
      this.templateApi.clone(this.template()!.id, result).subscribe({
        next: (workout) => {
          this.notify.success('Workout created');
          void this.router.navigate(['/workouts', workout.id]);
        },
        error: (err) => this.notify.error(errorMessage(err)),
      });
    });
  }

  back(): void {
    void this.router.navigate(['/templates']);
  }
}
