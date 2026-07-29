import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const ambassadorGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.hasRole('AMBASSADOR')) {
    return true;
  }

  router.navigate([authService.getDefaultRoute()]);
  return false;
};
