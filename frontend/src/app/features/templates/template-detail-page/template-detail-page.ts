import { Component, OnInit, inject } from '@angular/core';
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

  template: Template | null = null;
  loading = true;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/templates']);
      return;
    }
    this.templateApi.get(id).subscribe({
      next: (t) => {
        this.template = t;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.notify.error(errorMessage(err));
        void this.router.navigate(['/templates']);
      },
    });
  }

  edit(): void {
    if (this.template) {
      void this.router.navigate(['/templates', this.template.id, 'edit']);
    }
  }

  clone(): void {
    if (!this.template) {
      return;
    }
    const ref = this.dialog.open<CloneTemplateDialog, CloneTemplateDialogData>(
      CloneTemplateDialog,
      { data: { templateName: this.template.name }, width: '400px' },
    );
    ref.afterClosed().subscribe((result) => {
      if (!result || !this.template) {
        return;
      }
      this.templateApi.clone(this.template.id, result).subscribe({
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
