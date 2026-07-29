import { Component, OnInit, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { TemplateApi } from '../../../core/api/template-api.service';
import { TemplateVisibility } from '../../../core/models/enums';
import { Template } from '../../../core/models/template';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';
import {
  CloneTemplateDialog,
  CloneTemplateDialogData,
} from '../clone-template-dialog/clone-template-dialog';

@Component({
  selector: 'app-templates-page',
  templateUrl: './templates-page.html',
  standalone: false,
  styleUrl: './templates-page.scss',
})
export class TemplatesPage implements OnInit {
  private readonly templateApi = inject(TemplateApi);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);

  readonly showPublic = signal(false);
  readonly ownTemplates = signal<Template[]>([]);
  readonly publicTemplates = signal<Template[]>([]);
  readonly loading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.templateApi.list().subscribe({
      next: (items) => {
        this.ownTemplates.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notify.error(errorMessage(err, 'Failed to load templates'));
      },
    });
  }

  togglePublic(show: boolean): void {
    this.showPublic.set(show);
    if (show && this.publicTemplates().length === 0) {
      this.templateApi.list(TemplateVisibility.Public).subscribe({
        next: (items) => this.publicTemplates.set(items),
        error: (err) => this.notify.error(errorMessage(err)),
      });
    }
  }

  create(): void {
    void this.router.navigate(['/templates/new']);
  }

  view(id: string): void {
    void this.router.navigate(['/templates', id]);
  }

  edit(id: string): void {
    void this.router.navigate(['/templates', id, 'edit']);
  }

  delete(template: Template): void {
    if (!confirm(`Delete template "${template.name}"?`)) {
      return;
    }
    this.templateApi.delete(template.id).subscribe({
      next: () => {
        this.notify.success('Template deleted');
        this.load();
      },
      error: (err) => this.notify.error(errorMessage(err)),
    });
  }

  clone(template: Template): void {
    const ref = this.dialog.open<CloneTemplateDialog, CloneTemplateDialogData>(
      CloneTemplateDialog,
      { data: { templateName: template.name }, width: '400px' },
    );
    ref.afterClosed().subscribe((result) => {
      if (!result) {
        return;
      }
      this.templateApi.clone(template.id, result).subscribe({
        next: (workout) => {
          this.notify.success('Workout created from template');
          void this.router.navigate(['/workouts', workout.id]);
        },
        error: (err) => this.notify.error(errorMessage(err, 'Failed to clone template')),
      });
    });
  }
}
