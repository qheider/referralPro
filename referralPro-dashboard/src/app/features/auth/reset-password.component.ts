import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

type PageState = 'form' | 'missing-token' | 'submitting' | 'reset' | 'error';

/**
 * Public landing page for the password reset link users get emailed - see
 * EmailService.sendPasswordResetEmail, which points here with ?token={rawToken}. Mirrors
 * AcceptInvitationComponent's token-from-query-params / set-new-password pattern.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div class="text-center">
          <h2 class="mt-6 text-3xl font-extrabold text-gray-900">Reset Your Password</h2>
          <p class="mt-2 text-sm text-gray-600" *ngIf="state === 'form' || state === 'submitting'">
            Choose a new password for your account.
          </p>
        </div>

        <div class="rounded-md bg-red-50 p-4" *ngIf="state === 'missing-token'">
          <h3 class="text-sm font-medium text-red-800">Invalid reset link</h3>
          <p class="mt-2 text-sm text-red-700">
            This link is missing its reset token. Please use the link from your password reset email,
            or request a new one.
          </p>
          <a
            routerLink="/forgot-password"
            class="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
          >
            Request a new link
          </a>
        </div>

        <form
          *ngIf="state === 'form' || state === 'submitting'"
          [formGroup]="form"
          (ngSubmit)="submit()"
          class="mt-8 space-y-6"
        >
          <div class="rounded-md bg-red-50 p-4" *ngIf="submitError">
            <p class="text-sm text-red-700">{{ submitError }}</p>
          </div>

          <div class="space-y-4">
            <label class="block">
              <span class="block text-sm font-medium text-gray-700">New password</span>
              <input
                type="password"
                formControlName="newPassword"
                autocomplete="new-password"
                class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
              />
              <p
                class="mt-1 text-sm text-red-600"
                *ngIf="form.get('newPassword')?.invalid && form.get('newPassword')?.touched"
              >
                Password must be at least 8 characters.
              </p>
            </label>

            <label class="block">
              <span class="block text-sm font-medium text-gray-700">Confirm password</span>
              <input
                type="password"
                formControlName="confirmPassword"
                autocomplete="new-password"
                class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
              />
              <p
                class="mt-1 text-sm text-red-600"
                *ngIf="form.errors?.['passwordMismatch'] && form.get('confirmPassword')?.touched"
              >
                Passwords do not match.
              </p>
            </label>
          </div>

          <button
            type="submit"
            [disabled]="state === 'submitting' || form.invalid"
            class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ state === 'submitting' ? 'Resetting...' : 'Reset password' }}
          </button>
        </form>

        <div class="rounded-md bg-green-50 p-4" *ngIf="state === 'reset'">
          <h3 class="text-sm font-medium text-green-800">Password reset!</h3>
          <p class="mt-2 text-sm text-green-700">
            Your password has been changed. Redirecting to login...
          </p>
        </div>

        <div class="rounded-md bg-red-50 p-4" *ngIf="state === 'error'">
          <h3 class="text-sm font-medium text-red-800">Something went wrong</h3>
          <p class="mt-2 text-sm text-red-700">{{ submitError }}</p>
          <a
            routerLink="/forgot-password"
            class="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
          >
            Request a new link
          </a>
        </div>
      </div>
    </div>
  `
})
export class ResetPasswordComponent implements OnInit {
  readonly form: FormGroup;

  state: PageState = 'form';
  submitError = '';

  private token = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    this.form = this.fb.group(
      {
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required]
      },
      { validators: this.passwordMatchValidator }
    );
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';

    if (!this.token) {
      this.state = 'missing-token';
    }
  }

  submit(): void {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }

    this.state = 'submitting';
    this.submitError = '';

    this.authService.resetPassword(this.token, this.form.value.newPassword).subscribe({
      next: () => {
        this.state = 'reset';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (error: unknown) => {
        this.state = 'error';
        this.submitError = extractApiErrorMessage(error, 'This reset link is invalid or has expired.');
      }
    });
  }

  private passwordMatchValidator(group: FormGroup) {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }
}
