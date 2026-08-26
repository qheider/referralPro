import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, catchError, throwError, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { LoginRequest, LoginResponse, CurrentUserResponse, User, AcceptInvitationResponse, ResetPasswordResponse } from '../../shared/models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'jwt_token';
  private readonly USER_KEY = 'current_user';
  
  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();
  
  // Signal for reactive components
  public currentUserSignal = signal<User | null>(this.getUserFromStorage());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Login with username and password
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<ApiResponse<LoginResponse>>(`${environment.apiUrl}/auth/login`, credentials)
      .pipe(
        map(response => this.unwrapResponse(response, 'Login response did not include user data')),
        tap(response => {
          this.setToken(response.token);

          const user: User = {
            userId: response.userId,
            username: response.username,
            companyId: response.companyId,
            role: response.role
          };
          this.setUser(user);

          this.currentUserSubject.next(user);
          this.currentUserSignal.set(user);
        }),
        catchError(error => {
          console.error('Login failed:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Logout and clear stored data
   */
  logout(): void {
    this.clearToken();
    this.clearUser();
    this.currentUserSubject.next(null);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  /**
   * Get current user details from API
   */
  getCurrentUser(): Observable<CurrentUserResponse> {
    return this.http.get<ApiResponse<CurrentUserResponse>>(`${environment.apiUrl}/auth/me`)
      .pipe(
        map(response => this.unwrapResponse(response, 'Current user response did not include user data')),
        tap(response => {
          const user: User = {
            userId: response.userId,
            username: response.username,
            companyId: response.companyId,
            companyName: response.companyName,
            role: response.role
          };
          this.setUser(user);
          this.currentUserSubject.next(user);
          this.currentUserSignal.set(user);
        })
      );
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    
    // Check if token is expired
    return !this.isTokenExpired(token);
  }

  /**
   * Get stored JWT token
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Store JWT token
   */
  private setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  /**
   * Clear stored token
   */
  private clearToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
  }

  /**
   * Get user from local storage
   */
  private getUserFromStorage(): User | null {
    const userJson = localStorage.getItem(this.USER_KEY);
    if (userJson) {
      try {
        return JSON.parse(userJson);
      } catch (e) {
        console.error('Failed to parse user from storage:', e);
        return null;
      }
    }
    return null;
  }

  /**
   * Store user in local storage
   */
  private setUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  /**
   * Clear stored user
   */
  private clearUser(): void {
    localStorage.removeItem(this.USER_KEY);
  }

  /**
   * Check if token is expired
   */
  private isTokenExpired(token: string): boolean {
    try {
      const payload = this.parseJwt(token);
      if (!payload.exp) {
        return false;
      }
      
      const expirationDate = new Date(payload.exp * 1000);
      return expirationDate < new Date();
    } catch (e) {
      console.error('Failed to parse token:', e);
      return true;
    }
  }

  /**
   * Parse JWT token to extract payload
   */
  private parseJwt(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (e) {
      console.error('Failed to parse JWT:', e);
      return {};
    }
  }

  /**
   * Get current user value (synchronous)
   */
  getCurrentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  hasRole(role: string): boolean {
    return this.getCurrentUserValue()?.role === role;
  }

  /**
   * Verify email with token from registration
   */
  verifyEmail(token: string): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/auth/verify-email`, { token })
      .pipe(
        map(response => this.unwrapResponse(response, 'Email verification failed')),
        catchError(error => {
          console.error('Email verification failed:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Accept an ambassador (or other invitation-based) account invitation by setting a password.
   * Mirrors verifyEmail's shape - see AccountInvitationController#acceptInvitation.
   */
  acceptInvitation(token: string, password: string): Observable<AcceptInvitationResponse> {
    return this.http.post<ApiResponse<AcceptInvitationResponse>>(`${environment.apiUrl}/auth/accept-invitation`, { token, password })
      .pipe(
        map(response => this.unwrapResponse(response, 'Unable to accept this invitation')),
        catchError(error => {
          console.error('Accept invitation failed:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Request a password reset link. Mirrors verifyEmail's shape - see
   * PasswordResetController#forgotPassword. Always resolves with the same generic message
   * whether or not the email belongs to an account (anti-enumeration), so callers should show a
   * generic "check your email" panel rather than branching on the response.
   */
  forgotPassword(email: string): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/auth/forgot-password`, { email })
      .pipe(
        map(response => this.unwrapResponse(response, 'Unable to process this request')),
        catchError(error => {
          console.error('Forgot password request failed:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Reset a password using the token from the reset-link email. Mirrors acceptInvitation's shape
   * - see PasswordResetController#resetPassword.
   */
  resetPassword(token: string, newPassword: string): Observable<ResetPasswordResponse> {
    return this.http.post<ApiResponse<ResetPasswordResponse>>(`${environment.apiUrl}/auth/reset-password`, { token, newPassword })
      .pipe(
        map(response => this.unwrapResponse(response, 'Unable to reset this password')),
        catchError(error => {
          console.error('Reset password failed:', error);
          return throwError(() => error);
        })
      );
  }

  getDefaultRoute(): string {
    return this.getDefaultRouteForRole(this.getCurrentUserValue()?.role);
  }

  getDefaultRouteForRole(role?: string | null): string {
    return role === 'AMBASSADOR' ? '/ambassador' : '/dashboard';
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
