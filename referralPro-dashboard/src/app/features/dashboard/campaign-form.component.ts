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
            <span class="text-sm font-medium text-slate-700">Campaign name <span class="text-red-500">*</span></span>
            <input formControlName="name" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('name')" [class.border-slate-200]="!fieldError('name')" />
            <p *ngIf="fieldError('name')" class="text-xs text-red-600">{{ fieldError('name') }}</p>
          </label>

          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">Description</span>
            <textarea formControlName="description" rows="3" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"></textarea>
          </label>

          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">Landing page URL <span class="text-red-500">*</span></span>
            <input formControlName="landingPageUrl" placeholder="https://example.com/promo" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('landingPageUrl')" [class.border-slate-200]="!fieldError('landingPageUrl')" />
            <p *ngIf="fieldError('landingPageUrl')" class="text-xs text-red-600">{{ fieldError('landingPageUrl') }}</p>
          </label>

          <label class="flex items-center gap-2 md:col-span-2">
            <input type="checkbox" formControlName="directToLandingPageEnabled" class="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer" />
            <span class="text-sm font-medium text-slate-700">Send ambassadors directly to your landing page</span>
          </label>
          <p class="-mt-2 text-xs text-slate-400 md:col-span-2">
            Skips ReferralPro's own redirect and lead-capture page - the ambassador's link/QR go straight to the landing
            page URL above with a <code>?ref=</code> parameter, and you report registrations back via the conversion API.
            Requires a landing page URL; otherwise the default flow is used.
          </p>

          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">Qualifying conditions</span>
            <textarea formControlName="qualifyingConditions" rows="2" placeholder="e.g. referee must complete a paid signup within 30 days" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"></textarea>
          </label>

          <label class="space-y-2 md:col-span-2">
            <span class="text-sm font-medium text-slate-700">Incentive description</span>
            <textarea formControlName="incentiveDescription" rows="2" placeholder="What the referrer and referee get, in plain language" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500"></textarea>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Terms URL</span>
            <input formControlName="termsUrl" placeholder="https://example.com/terms" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Budget cap</span>
            <input formControlName="budgetCap" type="number" min="0" step="0.01" placeholder="Optional" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500" />
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referral start date <span class="text-red-500">*</span></span>
            <input formControlName="startDate" type="datetime-local" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('startDate')" [class.border-slate-200]="!fieldError('startDate')" />
            <p *ngIf="fieldError('startDate')" class="text-xs text-red-600">{{ fieldError('startDate') }}</p>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referral end date <span class="text-red-500">*</span></span>
            <input formControlName="endDate" type="datetime-local" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('endDate')" [class.border-slate-200]="!fieldError('endDate')" />
            <p *ngIf="fieldError('endDate')" class="text-xs text-red-600">{{ fieldError('endDate') }}</p>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Ambassador enrollment start <span class="text-red-500">*</span></span>
            <input formControlName="ambassadorEnrollmentStart" type="datetime-local" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('ambassadorEnrollmentStart')" [class.border-slate-200]="!fieldError('ambassadorEnrollmentStart')" />
            <p *ngIf="fieldError('ambassadorEnrollmentStart')" class="text-xs text-red-600">{{ fieldError('ambassadorEnrollmentStart') }}</p>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Ambassador enrollment end <span class="text-red-500">*</span></span>
            <p class="text-xs text-slate-400">Must be on or before the referral end date.</p>
            <input formControlName="ambassadorEnrollmentEnd" type="datetime-local" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('ambassadorEnrollmentEnd')" [class.border-slate-200]="!fieldError('ambassadorEnrollmentEnd')" />
            <p *ngIf="fieldError('ambassadorEnrollmentEnd')" class="text-xs text-red-600">{{ fieldError('ambassadorEnrollmentEnd') }}</p>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Reward type <span class="text-red-500">*</span></span>
            <select formControlName="rewardType" class="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-indigo-500">
              <option value="DISCOUNT_AMOUNT">Discount amount</option>
              <option value="DISCOUNT_PERCENTAGE">Discount percentage</option>
              <option value="CREDIT">Credit</option>
              <option value="POINTS">Points</option>
            </select>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Conversion event name <span class="text-red-500">*</span></span>
            <input formControlName="conversionEventName" placeholder="purchase_completed" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('conversionEventName')" [class.border-slate-200]="!fieldError('conversionEventName')" />
            <p *ngIf="fieldError('conversionEventName')" class="text-xs text-red-600">{{ fieldError('conversionEventName') }}</p>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referrer reward value <span class="text-red-500">*</span></span>
            <input formControlName="referrerRewardValue" type="number" min="0" step="0.01" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('referrerRewardValue')" [class.border-slate-200]="!fieldError('referrerRewardValue')" />
            <p *ngIf="fieldError('referrerRewardValue')" class="text-xs text-red-600">{{ fieldError('referrerRewardValue') }}</p>
          </label>

          <label class="space-y-2">
            <span class="text-sm font-medium text-slate-700">Referee reward value <span class="text-red-500">*</span></span>
            <input formControlName="refereeRewardValue" type="number" min="0" step="0.01" class="w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-indigo-500" [class.border-red-300]="fieldError('refereeRewardValue')" [class.border-slate-200]="!fieldError('refereeRewardValue')" />
            <p *ngIf="fieldError('refereeRewardValue')" class="text-xs text-red-600">{{ fieldError('refereeRewardValue') }}</p>
          </label>
        </div>

        <div class="mt-6 flex flex-wrap gap-3">
          <button type="submit" class="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60" [disabled]="isSaving">
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
      qualifyingConditions: [''],
      incentiveDescription: [''],
      termsUrl: ['', [Validators.maxLength(500)]],
      budgetCap: [null as number | null, [Validators.min(0)]],
      landingPageUrl: ['', [Validators.required, Validators.maxLength(500)]],
      directToLandingPageEnabled: [false],
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

  private static readonly FIELD_LABELS: Record<string, string> = {
    name: 'Campaign name',
    landingPageUrl: 'Landing page URL',
    startDate: 'Referral start date',
    endDate: 'Referral end date',
    ambassadorEnrollmentStart: 'Ambassador enrollment start',
    ambassadorEnrollmentEnd: 'Ambassador enrollment end',
    rewardType: 'Reward type',
    conversionEventName: 'Conversion event name',
    referrerRewardValue: 'Referrer reward value',
    refereeRewardValue: 'Referee reward value'
  };

  /** Field-level validation message for a touched, invalid control - drives the inline errors under each input. */
  fieldError(controlName: string): string | null {
    const control = this.form.get(controlName);
    if (!control || !control.touched || control.valid) {
      return null;
    }

    const label = CampaignFormComponent.FIELD_LABELS[controlName] ?? 'This field';
    if (control.hasError('required')) {
      return `${label} is required.`;
    }
    if (control.hasError('maxlength')) {
      return `${label} must be ${control.getError('maxlength').requiredLength} characters or fewer.`;
    }
    if (control.hasError('min')) {
      return `${label} must be ${control.getError('min').min} or greater.`;
    }
    return `${label} is invalid.`;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage = 'Please fix the highlighted fields below before creating the campaign.';
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
        qualifyingConditions: rawValue.qualifyingConditions || null,
        incentiveDescription: rawValue.incentiveDescription || null,
        termsUrl: rawValue.termsUrl || null,
        budgetCap: rawValue.budgetCap ?? null,
        landingPageUrl: rawValue.landingPageUrl ?? '',
        directToLandingPageEnabled: rawValue.directToLandingPageEnabled ?? false,
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
