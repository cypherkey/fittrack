import { Component, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth.service';
import { User } from '../../../core/models/user';

@Component({
  selector: 'app-settings-page',
  templateUrl: './settings-page.html',
  standalone: false,
  styleUrl: './settings-page.scss',
})
export class SettingsPage {
  private readonly auth = inject(AuthService);

  readonly apiBaseUrl = environment.apiBaseUrl;
  readonly user$: Observable<User | null> = this.auth.user$;
}