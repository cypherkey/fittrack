import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

const THEME_KEY = 'fittrack.theme';

export type ThemeMode = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly modeSubject = new BehaviorSubject<ThemeMode>(this.readInitial());
  readonly mode$ = this.modeSubject.asObservable();

  constructor() {
    this.apply(this.modeSubject.value);
  }

  get mode(): ThemeMode {
    return this.modeSubject.value;
  }

  toggle(): void {
    this.setMode(this.mode === 'dark' ? 'light' : 'dark');
  }

  setMode(mode: ThemeMode): void {
    localStorage.setItem(THEME_KEY, mode);
    this.modeSubject.next(mode);
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
