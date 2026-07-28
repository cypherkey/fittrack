import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-auth-callback-page',
  templateUrl: './auth-callback-page.html',
  standalone: false,
  styleUrl: './auth-callback-page.scss',
})
export class AuthCallbackPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  error: string | null = null;

  ngOnInit(): void {
    const hash = window.location.hash.startsWith('#')
      ? window.location.hash.slice(1)
      : window.location.hash;
    const params = new URLSearchParams(hash);
    const token = params.get('token');

    if (!token) {
      this.error = 'Missing token from Google sign-in.';
      return;
    }

    this.auth.acceptToken(token);
    history.replaceState(null, '', window.location.pathname);
    this.auth.loadMe().subscribe({
      next: (user) => {
        if (user) {
          void this.router.navigate(['/']);
        } else {
          this.error = 'Could not load your profile.';
        }
      },
      error: () => {
        this.error = 'Could not complete Google sign-in.';
      },
    });
  }
}