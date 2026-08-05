import { Component, inject, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { errorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-settings-page',
  templateUrl: './settings-page.html',
  standalone: false,
  styleUrl: './settings-page.scss',
})
export class SettingsPage {
  readonly auth = inject(AuthService);
  private readonly notify = inject(NotificationService);

  readonly apiBaseUrl =
    environment.apiBaseUrl || '(dev proxy → http://localhost:8080)';

  readonly savingPreference = signal(false);

  setUseMetric(useMetric: boolean): void {
    const user = this.auth.user();
    if (!user || this.savingPreference() || user.useMetric === useMetric) {
      return;
    }
    this.savingPreference.set(true);
    this.auth.updateMe({ useMetric }).subscribe({
      next: () => {
        this.savingPreference.set(false);
        this.notify.success(useMetric ? 'Using metric units' : 'Using imperial units');
      },
      error: (err) => {
        this.savingPreference.set(false);
        this.notify.error(errorMessage(err, 'Failed to update preference'));
        void this.auth.loadMe().subscribe();
      },
    });
  }
}