import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div class="max-w-md w-full space-y-8">
        <div class="text-center">
          <h2 class="mt-6 text-3xl font-extrabold text-gray-900">Verify Your Email</h2>
        </div>

        <div class="rounded-md bg-blue-50 p-4" *ngIf="!verificationComplete && !verificationError">
          <div class="flex">
            <div class="flex-shrink-0">
              <svg class="h-5 w-5 text-blue-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
              </svg>
            </div>
            <div class="ml-3">
              <p class="text-sm font-medium text-blue-800">
                {{ message }}
              </p>
            </div>
          </div>
        </div>

        <div class="rounded-md bg-green-50 p-4" *ngIf="verificationComplete && !verificationError">
          <div class="flex">
            <div class="flex-shrink-0">
              <svg class="h-5 w-5 text-green-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
            </div>
            <div class="ml-3">
              <h3 class="text-sm font-medium text-green-800">Email verified successfully!</h3>
              <p class="mt-2 text-sm text-green-700">
                Redirecting to login...
              </p>
            </div>
          </div>
        </div>

        <div class="rounded-md bg-red-50 p-4" *ngIf="verificationError">
          <div class="flex">
            <div class="flex-shrink-0">
              <svg class="h-5 w-5 text-red-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
              </svg>
            </div>
            <div class="ml-3">
              <h3 class="text-sm font-medium text-red-800">Verification failed</h3>
              <p class="mt-2 text-sm text-red-700">
                {{ verificationError }}
              </p>
              <button
                (click)="goToLogin()"
                class="mt-4 inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-red-700 bg-red-100 hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
              >
                Back to Login
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class VerifyEmailComponent implements OnInit {
  message = 'Verifying your email...';
  verificationComplete = false;
  verificationError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      
      if (!token) {
        this.verificationError = 'No verification token provided.';
        return;
      }

      // Call the verify-email API endpoint
      this.authService.verifyEmail(token).subscribe(
        (response) => {
          this.message = 'Email verified successfully!';
          this.verificationComplete = true;
          
          // Redirect to login after 2 seconds
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        },
        (error) => {
          console.error('Email verification failed:', error);
          this.verificationError = error.error?.message || 'An error occurred during verification. Please try again.';
          this.message = '';
        }
      );
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
