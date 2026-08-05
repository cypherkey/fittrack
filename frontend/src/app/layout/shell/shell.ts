import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { ThemeService } from '../../core/theme.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  exact: boolean;
  adminOnly?: boolean;
}

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

  private readonly allNavItems: NavItem[] = [
    { label: 'Dashboard', path: '/', icon: 'dashboard', exact: true },
    { label: 'Workouts', path: '/workouts', icon: 'fitness_center', exact: false },
    { label: 'Templates', path: '/templates', icon: 'view_list', exact: false },
    { label: 'Exercises', path: '/exercises', icon: 'sports_gymnastics', exact: false },
    { label: 'Users', path: '/users', icon: 'group', exact: false, adminOnly: true },
    { label: 'Settings', path: '/settings', icon: 'settings', exact: false },
  ];

  readonly navItems = computed(() => {
    const isAdmin = !!this.auth.user()?.admin;
    return this.allNavItems.filter((item) => !item.adminOnly || isAdmin);
  });

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