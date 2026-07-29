import { Component, OnInit, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { ThemeService } from '../../core/theme.service';

@Component({
  selector: 'app-shell',
  templateUrl: './shell.html',
  standalone: false,
  styleUrl: './shell.scss',
})
export class Shell implements OnInit {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);

  readonly sidenavOpened = signal(true);

  readonly navItems = [
    { label: 'Dashboard', path: '/', icon: 'dashboard', exact: true },
    { label: 'Workouts', path: '/workouts', icon: 'fitness_center', exact: false },
    { label: 'Templates', path: '/templates', icon: 'view_list', exact: false },
    { label: 'Exercises', path: '/exercises', icon: 'sports_gymnastics', exact: false },
    { label: 'Settings', path: '/settings', icon: 'settings', exact: false },
  ];

  ngOnInit(): void {
    this.auth.loadMe().subscribe();
  }

  toggleSidenav(): void {
    this.sidenavOpened.update((v) => !v);
  }

  logout(): void {
    this.auth.logout();
  }
}
