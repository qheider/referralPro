import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * HTTP Interceptor for adding JWT token to requests
 * and handling authentication errors
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // Get token from auth service
  const token = authService.getToken();
  console.log('authInterceptor: Processing request to', req.url, 'with token:', token ? 'YES' : 'NO');
  
  // Clone request and add Authorization header if token exists
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
  
  // Handle the request and catch authentication errors
  return next(authReq).pipe(
    catchError(error => {
      console.error('authInterceptor: HTTP error for', req.url, ':', error);
      // If 401 Unauthorized, logout and redirect to login
      if (error.status === 401) {
        authService.logout();
        router.navigate(['/login'], {
          queryParams: { returnUrl: router.url }
        });
      }
      
      return throwError(() => error);
    })
  );
};
