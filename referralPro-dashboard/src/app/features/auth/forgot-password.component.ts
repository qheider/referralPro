import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

type PageState = 'form' | 'submitting' | 'sent';

/**
 * Public landing page for the login page's "Forgot password?" link. Collects only an email
 * (see PasswordResetController#forgotPassword) and always shows the same generic "check your
 * email" panel on submit, whether or not the address belongs to an account - the backend response
 * never reveals account existence, so this page must not either.
 */
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div class="text-center">
          <h2 class="text-3xl font-extrabold text-gray-900">Forgot your password?</h2>
          <p class="mt-2 text-sm text-gray-600" *ngIf="state !== 'sent'">
            Enter the email address on your account and we'll send you a link to reset your password.
          </p>
        </div>

        <div class="bg-white py-8 px-6 shadow-xl rounded-2xl sm:px-10">
          <form *ngIf="state !== 'sent'" [formGroup]="form" (ngSubmit)="submit()" class="space-y-6">
            <div *ngIf="submitError" class="bg-red-50 border-l-4 border-red-500 p-4 rounded" role="alert">
              <p class="text-sm text-red-700">{{ submitError }}</p>
            </div>

            <div>
              <label for="email" class="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                autocomplete="email"
                class="appearance-none block w-full px-4 py-3 border border-gray-300 rounded-lg shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all duration-200"
                placeholder="admin@company.com"
              />
              <p
                class="mt-1 text-sm text-red-600"
                *ngIf="form.get('email')?.invalid && form.get('email')?.touched"
              >
                Enter a valid email address.
              </p>
            </div>

            <button
              type="submit"
              [disabled]="state === 'submitting' || form.invalid"
              class="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
            >
              {{ state === 'submitting' ? 'Sending...' : 'Send reset link' }}
            </button>
          </form>

          <div class="rounded-md bg-green-50 p-4" *ngIf="state === 'sent'">
            <h3 class="text-sm font-medium text-green-800">Check your email</h3>
            <p class="mt-2 text-sm text-green-700">
              If an account with that email exists, we've sent a link to reset your password. The link expires in 30 minutes.
            </p>
          </div>

          <div class="mt-6 text-center">
            <a routerLink="/login" class="text-sm font-medium text-indigo-600 hover:text-indigo-500 transition-colors">
              Back to login
            </a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ForgotPasswordComponent {
  readonly form: FormGroup;

  state: PageState = 'form';
  submitError = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.state = 'submitting';
    this.submitError = '';

    this.authService.forgotPassword(this.form.value.email).subscribe({
      next: () => {
        this.state = 'sent';
      },
      error: (error: unknown) => {
        // Even on an unexpected error, don't distinguish "not found" from anything else - show
        // the same generic confirmation rather than a distinct error state that could leak
        // whether the email is registered.
        this.state = 'sent';
        console.error('Forgot password request failed:', extractApiErrorMessage(error, 'Unknown error'));
      }
    });
  }
}
