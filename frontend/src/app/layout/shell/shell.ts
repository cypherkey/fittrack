import { Component, OnInit, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { User } from '../../core/models/user';
import { ThemeService } from '../../core/theme.service';

@Component({
  selector: 'app-shell',
  templateUrl: './shell.html',
  standalone: false,
  styleUrl: './shell.scss',
})
export class Shell implements OnInit {
  private readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);

  sidenavOpened = true;
  user$!: Observable<User | null>;

  readonly navItems = [
    { label: 'Dashboard', path: '/', icon: 'dashboard', exact: true },
    { label: 'Workouts', path: '/workouts', icon: 'fitness_center', exact: false },
    { label: 'Templates', path: '/templates', icon: 'view_list', exact: false },
    { label: 'Exercises', path: '/exercises', icon: 'sports_gymnastics', exact: false },
    { label: 'Settings', path: '/settings', icon: 'settings', exact: false },
  ];

  ngOnInit(): void {
    this.user$ = this.auth.user$;
    this.auth.loadMe().subscribe();
  }

  toggleSidenav(): void {
    this.sidenavOpened = !this.sidenavOpened;
  }

  logout(): void {
    this.auth.logout();
  }
}