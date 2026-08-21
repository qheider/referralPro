import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { AmbassadorApplicationService } from '../../core/services/ambassador-application.service';
import { CampaignService } from '../../core/services/campaign.service';
import { PublicCampaignResponse } from '../../shared/models/campaign.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

type PageState = 'loading' | 'unavailable' | 'error' | 'open' | 'submitted';

@Component({
  selector: 'app-campaign-join',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="mx-auto max-w-2xl space-y-6 px-4 py-12">
      <div *ngIf="state === 'loading'" class="rounded-3xl bg-white p-8 text-center shadow-sm">
        <p class="text-sm text-slate-500">Loading campaign...</p>
      </div>

      <div *ngIf="state === 'error'" class="rounded-3xl border border-red-200 bg-red-50 p-8 text-center shadow-sm">
        <h2 class="text-xl font-semibold text-red-800">Something went wrong</h2>
        <p class="mt-2 text-sm text-red-700">{{ errorMessage }}</p>
      </div>

      <div *ngIf="state === 'unavailable'" class="rounded-3xl bg-white p-8 text-center shadow-sm">
        <h2 class="text-xl font-semibold text-slate-900">{{ campaign?.campaignName || 'This campaign' }}</h2>
        <p class="mt-3 text-sm text-slate-600">
          {{ campaign?.unavailableReason || 'Ambassador enrollment is not currently open for this campaign.' }}
        </p>
      </div>

      <div *ngIf="state === 'submitted'" class="rounded-3xl bg-white p-8 text-center shadow-sm">
        <h2 class="text-xl font-semibold text-slate-900">Application submitted</h2>
        <p class="mt-3 text-sm text-slate-600">
          We've emailed you a confirmation, and let {{ campaign?.companyName }} know someone
          wants to join as an ambassador for {{ campaign?.campaignName }}. Once they approve your
          application, you'll get another email with a link to set your password and access your
          ambassador dashboard.
        </p>
      </div>

      <ng-container *ngIf="state === 'open'">
        <div class="rounded-3xl bg-white p-6 shadow-sm">
          <p class="text-sm font-semibold uppercase tracking-wide text-indigo-600">{{ campaign?.companyName }}</p>
          <h2 class="mt-1 text-2xl font-semibold text-slate-900">Join {{ campaign?.campaignName }}</h2>
          <p *ngIf="campaign?.description" class="mt-2 text-sm text-slate-500">{{ campaign?.description }}</p>
        </div>

        <div *ngIf="submitError" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {{ submitError }}
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="rounded-3xl bg-white p-6 shadow-sm">
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">First name</span>
              <input formControlName="firstName" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">Last name</span>
              <input formControlName="lastName" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">Email</span>
              <input formControlName="email" type="email" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">Phone (optional)</span>
              <input formControlName="phone" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">Display name (optional)</span>
              <input formControlName="displayName" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">Social platform (optional)</span>
              <input formControlName="socialMediaPlatform" placeholder="Instagram" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2">
              <span class="text-sm font-medium text-slate-700">Social handle (optional)</span>
              <input formControlName="socialMediaHandle" placeholder="&#64;yourhandle" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
            </label>

            <label class="space-y-2 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">Why do you want to be an ambassador? (optional)</span>
              <textarea formControlName="bio" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"></textarea>
            </label>
          </div>

          <div class="mt-6">
            <button type="submit" class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700" [disabled]="isSubmitting || form.invalid">
              {{ isSubmitting ? 'Submitting...' : 'Register as an ambassador' }}
            </button>
          </div>
        </form>
      </ng-container>
    </section>
  `
})
export class CampaignJoinComponent implements OnInit {
  readonly form;

  state: PageState = 'loading';
  campaign: PublicCampaignResponse | null = null;
  errorMessage = '';
  submitError = '';
  isSubmitting = false;

  private campaignCode = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private campaignService: CampaignService,
    private ambassadorApplicationService: AmbassadorApplicationService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      phone: [''],
      displayName: [''],
      socialMediaPlatform: [''],
      socialMediaHandle: [''],
      bio: ['']
    });
  }

  ngOnInit(): void {
    this.campaignCode = this.route.snapshot.paramMap.get('campaignCode') ?? '';
    if (!this.campaignCode) {
      this.state = 'error';
      this.errorMessage = 'No campaign code was provided.';
      return;
    }

    this.campaignService.resolveJoinLink(this.campaignCode).subscribe({
      next: campaign => {
        this.campaign = campaign;
        this.state = campaign.enrollmentOpen ? 'open' : 'unavailable';
        // No zone.js in this app - the HTTP response arrives outside Angular's change-detection
        // notifications, so the state mutation above needs an explicit tick (see the same pattern
        // in dashboard.component.ts) or the view stays stuck on the previous state.
        this.cdr.markForCheck();
      },
      error: (error: unknown) => {
        this.state = 'error';
        this.errorMessage = extractApiErrorMessage(error, 'This campaign link is invalid or no longer available.');
        this.cdr.markForCheck();
      }
    });
  }

  submit(): void {
    if (this.form.invalid || !this.campaign) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.isSubmitting = true;
    this.submitError = '';

    this.ambassadorApplicationService
      .apply(this.campaign.companyId, this.campaignCode, {
        firstName: raw.firstName ?? '',
        lastName: raw.lastName ?? '',
        email: raw.email ?? '',
        phone: raw.phone || null,
        displayName: raw.displayName || null,
        bio: raw.bio || null,
        socialMediaPlatform: raw.socialMediaPlatform || null,
        socialMediaHandle: raw.socialMediaHandle || null
      })
      .pipe(finalize(() => {
        this.isSubmitting = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: () => {
          this.state = 'submitted';
          this.cdr.markForCheck();
        },
        error: (error: unknown) => {
          this.submitError = extractApiErrorMessage(error, 'Unable to submit application.');
          this.cdr.markForCheck();
        }
      });
  }
}
