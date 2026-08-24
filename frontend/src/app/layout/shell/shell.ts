import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ThemeService } from '../../core/theme.service';

interface NavItem {
  label: string;
  path?: string;
  icon: string;
  exact?: boolean;
  adminOnly?: boolean;
  children?: NavItem[];
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
  private readonly router = inject(Router);

  readonly sidenavOpened = signal(true);
  readonly workoutsExpanded = signal(true);

  private readonly allNavItems: NavItem[] = [
    { label: 'Dashboard', path: '/', icon: 'dashboard', exact: true },
    {
      label: 'Workouts',
      path: '/workouts',
      icon: 'fitness_center',
      exact: false,
      children: [
        { label: 'My Workouts', path: '/workouts', icon: 'person', exact: true },
        { label: 'My Team Workouts', path: '/workouts/team', icon: 'groups', exact: true },
      ],
    },
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
    this.syncWorkoutsExpanded(this.router.url);
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe((e) => {
      this.syncWorkoutsExpanded(e.urlAfterRedirects);
    });
  }

  private syncWorkoutsExpanded(url: string): void {
    if (url === '/workouts' || url.startsWith('/workouts/') || url.startsWith('/workouts?')) {
      this.workoutsExpanded.set(true);
    }
  }

  toggleSidenav(): void {
    this.sidenavOpened.update((v) => !v);
  }

  toggleWorkoutsMenu(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const next = !this.workoutsExpanded();
    this.workoutsExpanded.set(next);
    if (next) {
      void this.router.navigate(['/workouts']);
    }
  }

  logout(): void {
    this.auth.logout();
  }
}