import { Injectable, signal } from '@angular/core';

const THEME_KEY = 'fittrack.theme';

export type ThemeMode = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly mode = signal<ThemeMode>(this.readInitial());

  constructor() {
    this.apply(this.mode());
  }

  toggle(): void {
    this.setMode(this.mode() === 'dark' ? 'light' : 'dark');
  }

  setMode(mode: ThemeMode): void {
    localStorage.setItem(THEME_KEY, mode);
    this.mode.set(mode);
    this.apply(mode);
  }

  private readInitial(): ThemeMode {
    const stored = localStorage.getItem(THEME_KEY);
    return stored === 'dark' ? 'dark' : 'light';
  }

  private apply(mode: ThemeMode): void {
    document.body.style.colorScheme = mode;
  }
}
