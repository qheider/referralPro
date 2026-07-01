import { ActivatedRoute, Router } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authService: {
    isAuthenticated: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigate: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authService = {
      isAuthenticated: vi.fn().mockReturnValue(false),
      login: vi.fn()
    };

    router = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: {
                returnUrl: '/dashboard/campaigns/7'
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should mark fields touched instead of submitting an invalid form', () => {
    component.onSubmit();

    expect(authService.login).not.toHaveBeenCalled();
    expect(component.loginForm.get('username')?.touched).toBe(true);
    expect(component.loginForm.get('password')?.touched).toBe(true);
  });

  it('should navigate to the returnUrl after a successful login', () => {
    authService.login.mockReturnValue(
      of({
        token: 'jwt-token',
        userId: 1,
        username: 'admin@company.com',
        companyId: 1,
        role: 'COMPANY_ADMIN'
      })
    );

    component.loginForm.setValue({
      username: 'admin@company.com',
      password: 'password123',
      rememberMe: false
    });

    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      username: 'admin@company.com',
      password: 'password123'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/campaigns/7']);
    expect(component.isLoading).toBe(false);
  });

  it('should show a wrapped backend error message for non-401 failures', () => {
    authService.login.mockReturnValue(
      throwError(() => ({
        status: 500,
        error: {
          message: 'Backend login failed'
        }
      }))
    );

    component.loginForm.setValue({
      username: 'admin@company.com',
      password: 'password123',
      rememberMe: false
    });

    component.onSubmit();

    expect(component.errorMessage).toBe('Backend login failed');
    expect(component.isLoading).toBe(false);
  });
});
