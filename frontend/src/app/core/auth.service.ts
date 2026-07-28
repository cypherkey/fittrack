import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, User } from './models/user';
import { TokenStorage } from './token-storage';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSubject = new BehaviorSubject<User | null>(null);
  readonly user$ = this.userSubject.asObservable();

  constructor(
    private readonly http: HttpClient,
    private readonly tokens: TokenStorage,
    private readonly router: Router,
  ) {}

  get currentUser(): User | null {
    return this.userSubject.value;
  }

  isAuthenticated(): boolean {
    return this.tokens.hasToken();
  }

  login(credentials: LoginRequest): Observable<User> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/api/v1/auth/login`, credentials)
      .pipe(
        tap((res) => {
          this.tokens.setToken(res.token);
          this.userSubject.next({ ...res.user, admin: res.user.admin ?? false });
        }),
        map((res) => res.user),
      );
  }

  acceptToken(token: string): void {
    this.tokens.setToken(token);
  }

  loadMe(): Observable<User | null> {
    if (!this.tokens.hasToken()) {
      this.userSubject.next(null);
      return of(null);
    }
    return this.http.get<User>(`${environment.apiBaseUrl}/api/v1/me`).pipe(
      map((user) => ({ ...user, admin: user.admin ?? false })),
      tap((user) => this.userSubject.next(user)),
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
    );
  }

  logout(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  googleLoginUrl(): string {
    return `${environment.apiBaseUrl}/oauth2/authorization/google`;
  }

  private clearSession(): void {
    this.tokens.clearToken();
    this.userSubject.next(null);
  }
}
