import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { ReferralLeadService } from '../../core/services/referral-lead.service';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

type PageState = 'form' | 'submitted' | 'error';

/**
 * Public registration page a referred visitor lands on after clicking an ambassador's referral
 * link (see ReferralClickService's default /refer/{token} destination). Submits to the existing
 * lead-capture API, which creates the referred-user record and kicks off the outbox pipeline that
 * pushes it to the company's own system - see ReferralLeadService/ApiSubmissionDispatchService.
 */
@Component({
  selector: 'app-campaign-refer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-xl space-y-6 px-4 py-12">
      <div *ngIf="state === 'error'" class="rounded-3xl border border-red-200 bg-red-50 p-8 text-center shadow-sm">
        <h2 class="text-xl font-semibold text-red-800">This link is invalid</h2>
        <p class="mt-2 text-sm text-red-700">{{ errorMessage }}</p>
      </div>

      <div *ngIf="state === 'submitted'" class="rounded-3xl bg-white p-8 text-center shadow-sm">
        <h2 class="text-xl font-semibold text-slate-900">You're in!</h2>
        <p class="mt-3 text-sm text-slate-600">
          Thanks for signing up - we've registered your referral.
        </p>
      </div>

      <ng-container *ngIf="state === 'form'">
        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <h2 class="text-2xl font-semibold text-slate-900">Sign up</h2>
          <p class="mt-2 text-sm text-slate-500">You were referred by someone you know. Enter your details to continue.</p>
        </div>

        <div *ngIf="submitError" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {{ submitError }}
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="rounded-3xl bg-white p-6 shadow-sm">
          <div class="grid gap-4">
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">Name</span>
              <input formControlName="name" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">Email</span>
              <input formControlName="email" type="email" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>
          </div>

          <div class="mt-6">
            <button type="submit" class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700" [disabled]="isSubmitting || form.invalid">
              {{ isSubmitting ? 'Submitting...' : 'Continue' }}
            </button>
          </div>
        </form>
      </ng-container>
    </section>
  `
})
export class CampaignReferComponent implements OnInit {
  readonly form;

  state: PageState = 'form';
  errorMessage = '';
  submitError = '';
  isSubmitting = false;

  private token = '';
  private sessionId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private referralLeadService: ReferralLeadService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]]
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.sessionId = this.route.snapshot.queryParamMap.get('s');

    if (!this.token) {
      this.state = 'error';
      this.errorMessage = 'No referral link was provided.';
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.isSubmitting = true;
    this.submitError = '';

    this.referralLeadService
      .submitLead(this.token, this.sessionId, {
        name: raw.name ?? '',
        email: raw.email ?? ''
      })
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: () => {
          this.state = 'submitted';
        },
        error: (error: unknown) => {
          this.submitError = extractApiErrorMessage(error, 'Unable to submit registration.');
        }
      });
  }
}
