import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AmbassadorPortalService } from '../../core/services/ambassador-portal.service';
import { AmbassadorCampaignDetail } from '../../shared/models/ambassador-portal.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';
import { ReferralQrCodeComponent } from '../../shared/components/referral-qr-code.component';

@Component({
  selector: 'app-ambassador-campaign-detail',
  standalone: true,
  imports: [CommonModule, ReferralQrCodeComponent],
  template: `
    <div class="space-y-6" *ngIf="campaign; else stateBlock">
      <div>
        <p class="text-xs uppercase tracking-[0.3em] text-cyan-400">{{ campaign.status }}</p>
        <h2 class="mt-2 text-2xl font-semibold">{{ campaign.campaignName }}</h2>
        <p class="mt-2 text-sm text-slate-400">{{ campaign.description || 'No description available.' }}</p>
      </div>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-xl border border-slate-800 bg-slate-950 p-4" *ngFor="let item of metrics">
          <p class="text-sm text-slate-400">{{ item.label }}</p>
          <p class="mt-2 text-2xl font-semibold">{{ item.value }}</p>
        </div>
      </div>

      <div class="grid gap-4 xl:grid-cols-2">
        <section class="rounded-xl border border-slate-800 bg-slate-950 p-5">
          <h3 class="font-medium">Offer summary</h3>
          <dl class="mt-4 space-y-3 text-sm">
            <div class="flex justify-between gap-4"><dt class="text-slate-400">Reward type</dt><dd>{{ campaign.rewardType }}</dd></div>
            <div class="flex justify-between gap-4"><dt class="text-slate-400">Referrer reward</dt><dd>{{ campaign.referrerRewardValue }}</dd></div>
            <div class="flex justify-between gap-4"><dt class="text-slate-400">Referee reward</dt><dd>{{ campaign.refereeRewardValue }}</dd></div>
            <div class="flex justify-between gap-4"><dt class="text-slate-400">Conversion event</dt><dd>{{ campaign.conversionEventName }}</dd></div>
            <div class="flex justify-between gap-4"><dt class="text-slate-400">Starts</dt><dd>{{ campaign.startDate | date:'medium' }}</dd></div>
            <div class="flex justify-between gap-4"><dt class="text-slate-400">Ends</dt><dd>{{ campaign.endDate | date:'medium' }}</dd></div>
          </dl>
        </section>

        <section class="rounded-xl border border-slate-800 bg-slate-950 p-5">
          <h3 class="font-medium">Your referral link</h3>
          <p class="mt-4 break-all text-sm text-cyan-200">{{ campaign.referralLink.referralUrl }}</p>
          <p class="mt-4 text-sm text-slate-400">Landing page: {{ campaign.landingPageUrl }}</p>
          <app-referral-qr-code class="mt-4 block" [referralUrl]="campaign.referralLink.referralUrl" />
        </section>
      </div>
    </div>

    <ng-template #stateBlock>
      <p *ngIf="errorMessage" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ errorMessage }}</p>
    </ng-template>
  `
})
export class AmbassadorCampaignDetailComponent implements OnInit {
  campaign: AmbassadorCampaignDetail | null = null;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private ambassadorPortalService: AmbassadorPortalService
  ) {}

  ngOnInit(): void {
    const campaignId = Number(this.route.snapshot.paramMap.get('campaignId'));
    this.ambassadorPortalService.getCampaign(campaignId).subscribe({
      next: campaign => {
        this.campaign = campaign;
      },
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to load campaign details.');
      }
    });
  }

  get metrics(): { label: string; value: string | number }[] {
    if (!this.campaign) {
      return [];
    }

    return [
      { label: 'Clicks', value: this.campaign.clickCount },
      { label: 'Registrations', value: this.campaign.registrationCount },
      { label: 'Bookings started', value: this.campaign.bookingStartedCount },
      { label: 'Completed rentals', value: this.campaign.completedRentalCount },
      { label: 'Registration rate', value: `${this.campaign.registrationConversionRate}%` },
      { label: 'Rental rate', value: `${this.campaign.rentalConversionRate}%` }
    ];
  }
}
