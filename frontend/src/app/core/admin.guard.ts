import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const current = auth.user();
  if (current) {
    return current.admin ? true : router.createUrlTree(['/']);
  }

  return auth.loadMe().pipe(
    map((user) => (user?.admin ? true : router.createUrlTree(['/']))),
  );
};