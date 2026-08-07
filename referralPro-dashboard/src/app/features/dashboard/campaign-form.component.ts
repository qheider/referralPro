import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CampaignService } from '../../core/services/campaign.service';
import { RewardType } from '../../shared/models/campaign.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-campaign-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="mx-auto max-w-4xl space-y-6">
      <div class="rounded-3xl bg-white p-6 shadow-sm">
        <p class="text-sm font-semibold uppercase tracking-wide text-indigo-600">Create</p>
        <h2 class="mt-1 text-2xl font-semibold text-slate-900">New campaign</h2>
        <p class="mt-2 text-sm text-slate-500">
          Launch a referral marketing campaign for your company. Referral links can be generated for
          referrers or ambassadors once the campaign is live.
        </p>
      </div>

      <div *ngIf="errorMessage" class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {{ errorMessage }}
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()" class="rounded-3xl bg-white p-6 shadow-sm">
        <div class="grid gap-4 md:grid-cols-2">
          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">Campaign name</span>
            <input formControlName="name" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">Description</span>
            <textarea formControlName="description" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"></textarea>
          </label>

          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">Landing page URL</span>
            <input formControlName="landingPageUrl" placeholder="https://example.com/promo" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referral start date</span>
            <input formControlName="startDate" type="datetime-local" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referral end date</span>
            <input formControlName="endDate" type="datetime-local" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Ambassador enrollment start</span>
            <input formControlName="ambassadorEnrollmentStart" type="datetime-local" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Ambassador enrollment end</span>
            <p class="text-xs text-slate-400">Must be on or before the referral end date.</p>
            <input formControlName="ambassadorEnrollmentEnd" type="datetime-local" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Reward type</span>
            <select formControlName="rewardType" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500">
              <option value="DISCOUNT_AMOUNT">Discount amount</option>
              <option value="DISCOUNT_PERCENTAGE">Discount percentage</option>
              <option value="CREDIT">Credit</option>
              <option value="POINTS">Points</option>
            </select>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Conversion event name</span>
            <input formControlName="conversionEventName" placeholder="purchase_completed" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referrer reward value</span>
            <input formControlName="referrerRewardValue" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referee reward value</span>
            <input formControlName="refereeRewardValue" type="number" min="0" step="0.01" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>
        </div>

        <div class="mt-6 flex flex-wrap gap-3">
          <button type="submit" class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700" [disabled]="isSaving || form.invalid">
            {{ isSaving ? 'Creating...' : 'Create campaign' }}
          </button>
          <a routerLink="/dashboard/overview" class="rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Cancel</a>
        </div>
      </form>
    </section>
  `
})
export class CampaignFormComponent {
  readonly form;

  isSaving = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private campaignService: CampaignService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      landingPageUrl: ['', [Validators.required, Validators.maxLength(500)]],
      startDate: ['', [Validators.required]],
      endDate: ['', [Validators.required]],
      ambassadorEnrollmentStart: ['', [Validators.required]],
      ambassadorEnrollmentEnd: ['', [Validators.required]],
      rewardType: ['DISCOUNT_AMOUNT' as RewardType, [Validators.required]],
      conversionEventName: ['', [Validators.required, Validators.maxLength(150)]],
      referrerRewardValue: [0, [Validators.required, Validators.min(0)]],
      refereeRewardValue: [0, [Validators.required, Validators.min(0)]]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const companyId = this.authService.getCurrentUserValue()?.companyId;
    if (!companyId) {
      this.errorMessage = 'Unable to determine the current company. Please sign in again.';
      return;
    }

    const rawValue = this.form.getRawValue();

    if (rawValue.endDate && rawValue.startDate && rawValue.endDate <= rawValue.startDate) {
      this.errorMessage = 'Campaign end date must be after the start date.';
      return;
    }

    if (
      rawValue.ambassadorEnrollmentEnd &&
      rawValue.ambassadorEnrollmentStart &&
      rawValue.ambassadorEnrollmentEnd <= rawValue.ambassadorEnrollmentStart
    ) {
      this.errorMessage = 'Ambassador enrollment end date must be after its start date.';
      return;
    }

    if (rawValue.ambassadorEnrollmentEnd && rawValue.endDate && rawValue.ambassadorEnrollmentEnd > rawValue.endDate) {
      this.errorMessage = "Ambassador enrollment must end on or before the campaign's referral end date.";
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';

    this.campaignService
      .createCampaign(companyId, {
        name: rawValue.name ?? '',
        description: rawValue.description || null,
        landingPageUrl: rawValue.landingPageUrl ?? '',
        startDate: rawValue.startDate ?? '',
        endDate: rawValue.endDate ?? '',
        ambassadorEnrollmentStart: rawValue.ambassadorEnrollmentStart ?? '',
        ambassadorEnrollmentEnd: rawValue.ambassadorEnrollmentEnd ?? '',
        rewardType: (rawValue.rewardType as RewardType) ?? 'DISCOUNT_AMOUNT',
        referrerRewardValue: rawValue.referrerRewardValue ?? 0,
        refereeRewardValue: rawValue.refereeRewardValue ?? 0,
        conversionEventName: rawValue.conversionEventName ?? ''
      })
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: campaign => {
          this.router.navigate(['/dashboard/campaigns', campaign.campaignId]);
        },
        error: (error: unknown) => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to create campaign.');
        }
      });
  }
}
