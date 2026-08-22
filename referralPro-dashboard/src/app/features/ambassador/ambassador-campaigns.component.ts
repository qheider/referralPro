import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AmbassadorPortalService } from '../../core/services/ambassador-portal.service';
import { AmbassadorCampaignOverview } from '../../shared/models/ambassador-portal.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-campaigns',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="space-y-4">
      <div>
        <h2 class="text-2xl font-semibold">Assigned campaigns</h2>
        <p class="text-sm text-slate-400">Access each personal referral link and monitor campaign-level performance.</p>
      </div>

      <p *ngIf="errorMessage" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ errorMessage }}</p>

      <div class="space-y-4" *ngIf="campaigns.length; else noCampaigns">
        <article *ngFor="let campaign of campaigns" class="rounded-xl border border-slate-800 bg-slate-950 p-5">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h3 class="text-lg font-semibold">{{ campaign.campaignName }}</h3>
              <p class="mt-1 text-sm text-slate-400">{{ campaign.description || 'No description available.' }}</p>
              <p class="mt-2 text-xs uppercase tracking-wide text-slate-500">{{ campaign.status }} · {{ campaign.rewardType }}</p>
            </div>
            <a [routerLink]="['/ambassador/campaigns', campaign.campaignId]" class="rounded-md border border-cyan-500 px-3 py-2 text-sm text-cyan-200">View details</a>
          </div>

          <div class="mt-4 grid gap-3 md:grid-cols-4">
            <div class="rounded-lg border border-slate-800 p-3">
              <p class="text-xs uppercase tracking-wide text-slate-500">Clicks</p>
              <p class="mt-2 text-xl font-semibold">{{ campaign.clickCount }}</p>
            </div>
            <div class="rounded-lg border border-slate-800 p-3">
              <p class="text-xs uppercase tracking-wide text-slate-500">Registrations</p>
              <p class="mt-2 text-xl font-semibold">{{ campaign.registrationCount }}</p>
            </div>
            <div class="rounded-lg border border-slate-800 p-3">
              <p class="text-xs uppercase tracking-wide text-slate-500">Completed rentals</p>
              <p class="mt-2 text-xl font-semibold">{{ campaign.completedRentalCount }}</p>
            </div>
            <div class="rounded-lg border border-slate-800 p-3">
              <p class="text-xs uppercase tracking-wide text-slate-500">Registration rate</p>
              <p class="mt-2 text-xl font-semibold">{{ campaign.registrationConversionRate }}%</p>
            </div>
          </div>

          <div class="mt-4 rounded-lg border border-slate-800 p-4">
            <p class="text-xs uppercase tracking-wide text-slate-500">Referral link</p>
            <p class="mt-2 break-all text-sm text-cyan-200">{{ campaign.referralLink.referralUrl }}</p>
          </div>
        </article>
      </div>
    </div>

    <ng-template #noCampaigns>
      <p class="rounded-xl border border-slate-800 bg-slate-950 px-4 py-6 text-sm text-slate-400">No campaigns are assigned to your account yet.</p>
    </ng-template>
  `
})
export class AmbassadorCampaignsComponent implements OnInit {
  campaigns: AmbassadorCampaignOverview[] = [];
  errorMessage = '';

  constructor(private ambassadorPortalService: AmbassadorPortalService) {}

  ngOnInit(): void {
    this.ambassadorPortalService.listCampaigns().subscribe({
      next: campaigns => {
        this.campaigns = campaigns;
      },
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to load assigned campaigns.');
      }
    });
  }
}
