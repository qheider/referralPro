import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AmbassadorPortalService } from '../../core/services/ambassador-portal.service';
import { AmbassadorCampaignOverview, AmbassadorReferralHistoryResponse } from '../../shared/models/ambassador-portal.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-referrals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      <div class="flex flex-wrap items-end gap-3">
        <div>
          <label class="mb-1 block text-sm text-slate-400">Campaign</label>
          <select [(ngModel)]="selectedCampaignId" class="rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm">
            <option [ngValue]="undefined">All campaigns</option>
            <option *ngFor="let campaign of campaigns" [ngValue]="campaign.campaignId">{{ campaign.campaignName }}</option>
          </select>
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-400">Status</label>
          <select [(ngModel)]="selectedStatus" class="rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm">
            <option value="">All statuses</option>
            <option *ngFor="let status of statuses" [value]="status">{{ status }}</option>
          </select>
        </div>
        <button type="button" class="rounded-md bg-cyan-500 px-4 py-2 text-sm font-medium text-slate-950" (click)="loadReferrals()">Apply filters</button>
      </div>

      <p *ngIf="errorMessage" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ errorMessage }}</p>

      <div class="rounded-xl border border-slate-800" *ngIf="referrals">
        <div class="grid grid-cols-[1.5fr_1fr_1fr_1fr_1fr] gap-4 px-4 py-3 text-xs uppercase tracking-wide text-slate-500">
          <span>Customer</span>
          <span>Campaign</span>
          <span>Status</span>
          <span>Registered</span>
          <span>Discount</span>
        </div>
        <div *ngFor="let referral of referrals.content" class="grid grid-cols-[1.5fr_1fr_1fr_1fr_1fr] gap-4 border-t border-slate-800 px-4 py-3 text-sm">
          <div>
            <p class="font-medium">{{ referral.customerName }}</p>
            <p class="text-slate-400">{{ referral.customerEmail || 'No email available' }}</p>
          </div>
          <span>{{ referral.campaignName }}</span>
          <span>{{ referral.status }}</span>
          <span>{{ referral.registeredAt ? (referral.registeredAt | date:'mediumDate') : '—' }}</span>
          <span>{{ referral.discountAmount ?? '—' }} {{ referral.currency || '' }}</span>
        </div>
      </div>
    </div>
  `
})
export class AmbassadorReferralsComponent implements OnInit {
  campaigns: AmbassadorCampaignOverview[] = [];
  referrals: AmbassadorReferralHistoryResponse | null = null;
  selectedCampaignId: number | undefined;
  selectedStatus = '';
  errorMessage = '';
  readonly statuses = ['REGISTERED', 'BOOKING_STARTED', 'BOOKING_CONFIRMED', 'RENTAL_STARTED', 'COMPLETED', 'CONVERTED', 'REJECTED', 'CANCELLED'];

  constructor(private ambassadorPortalService: AmbassadorPortalService) {}

  ngOnInit(): void {
    this.ambassadorPortalService.listCampaigns().subscribe({
      next: campaigns => {
        this.campaigns = campaigns;
      }
    });
    this.loadReferrals();
  }

  loadReferrals(): void {
    this.ambassadorPortalService.listReferrals({
      campaignId: this.selectedCampaignId,
      status: this.selectedStatus || undefined
    }).subscribe({
      next: referrals => {
        this.referrals = referrals;
      },
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to load referrals.');
      }
    });
  }
}
