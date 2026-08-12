import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

type PageState = 'form' | 'missing-token' | 'submitting' | 'accepted' | 'error';

/**
 * Public landing page for the invitation link ambassadors (and any future invitation-based
 * account type) get emailed - see EmailService.sendAmbassadorInvitationEmail, which points here
 * with ?token={rawToken}. The invited DashboardUser is created with an unusable placeholder
 * password (AmbassadorAdminService.provisionAmbassadorAccount); this page is where the invitee
 * actually sets their real one via POST /api/auth/accept-invitation, mirroring
 * verify-email.component.ts's token-from-query-params pattern.
 */
@Component({
  selector: 'app-accept-invitation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div class="text-center">
          <h2 class="mt-6 text-3xl font-extrabold text-gray-900">Set Up Your Account</h2>
          <p class="mt-2 text-sm text-gray-600" *ngIf="state === 'form' || state === 'submitting'">
            Choose a password to activate your ambassador account and log in.
          </p>
        </div>

        <div class="rounded-md bg-red-50 p-4" *ngIf="state === 'missing-token'">
          <h3 class="text-sm font-medium text-red-800">Invalid invitation link</h3>
          <p class="mt-2 text-sm text-red-700">
            This link is missing its invitation token. Please use the link from your invitation email,
            or ask the person who invited you to resend it.
          </p>
          <a
            routerLink="/login"
            class="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
          >
            Back to Login
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
                formControlName="password"
                autocomplete="new-password"
                class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"
              />
              <p
                class="mt-1 text-sm text-red-600"
                *ngIf="form.get('password')?.invalid && form.get('password')?.touched"
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
            {{ state === 'submitting' ? 'Setting password...' : 'Activate account' }}
          </button>
        </form>

        <div class="rounded-md bg-green-50 p-4" *ngIf="state === 'accepted'">
          <h3 class="text-sm font-medium text-green-800">Account activated!</h3>
          <p class="mt-2 text-sm text-green-700">
            Your password has been set. Redirecting to login...
          </p>
        </div>

        <div class="rounded-md bg-red-50 p-4" *ngIf="state === 'error'">
          <h3 class="text-sm font-medium text-red-800">Something went wrong</h3>
          <p class="mt-2 text-sm text-red-700">{{ submitError }}</p>
          <a
            routerLink="/login"
            class="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
          >
            Back to Login
          </a>
        </div>
      </div>
    </div>
  `
})
export class AcceptInvitationComponent implements OnInit {
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
        password: ['', [Validators.required, Validators.minLength(8)]],
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

    this.authService.acceptInvitation(this.token, this.form.value.password).subscribe({
      next: () => {
        this.state = 'accepted';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (error) => {
        this.state = 'error';
        this.submitError = extractApiErrorMessage(error, 'Unable to accept this invitation. It may have expired or already been used.');
      }
    });
  }

  private passwordMatchValidator(group: FormGroup) {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }
}
