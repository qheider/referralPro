import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AmbassadorPortalService } from '../../core/services/ambassador-portal.service';
import { AmbassadorDashboardResponse } from '../../shared/models/ambassador-portal.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-semibold">Welcome, {{ dashboard?.displayName || 'Ambassador' }}</h2>
          <p class="text-sm text-slate-400">Track assigned campaigns, referral activity, and conversion performance.</p>
        </div>
        <a routerLink="/ambassador/campaigns" class="rounded-md bg-cyan-500 px-4 py-2 text-sm font-medium text-slate-950">View campaigns</a>
      </div>

      <p *ngIf="errorMessage" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ errorMessage }}</p>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-5" *ngIf="dashboard">
        <div class="rounded-xl border border-slate-800 bg-slate-950 p-4" *ngFor="let stat of stats">
          <p class="text-sm text-slate-400">{{ stat.label }}</p>
          <p class="mt-2 text-2xl font-semibold">{{ stat.value }}</p>
        </div>
      </div>

      <div class="rounded-xl border border-slate-800" *ngIf="dashboard">
        <div class="border-b border-slate-800 px-4 py-3">
          <h3 class="font-medium">Recent referrals</h3>
        </div>
        <div *ngIf="dashboard.recentReferrals.length; else noReferrals">
          <div class="grid grid-cols-[1.5fr_1fr_1fr_1fr] gap-4 px-4 py-3 text-xs uppercase tracking-wide text-slate-500">
            <span>Customer</span>
            <span>Campaign</span>
            <span>Status</span>
            <span>Converted</span>
          </div>
          <div *ngFor="let referral of dashboard.recentReferrals" class="grid grid-cols-[1.5fr_1fr_1fr_1fr] gap-4 border-t border-slate-800 px-4 py-3 text-sm">
            <div>
              <p class="font-medium">{{ referral.customerName }}</p>
              <p class="text-slate-400">{{ referral.customerEmail || 'No email available' }}</p>
            </div>
            <span>{{ referral.campaignName }}</span>
            <span>{{ referral.status }}</span>
            <span>{{ referral.convertedAt ? (referral.convertedAt | date:'mediumDate') : '—' }}</span>
          </div>
        </div>
      </div>

      <ng-template #noReferrals>
        <p class="px-4 py-6 text-sm text-slate-400">No referrals have been attributed to your links yet.</p>
      </ng-template>
    </div>
  `
})
export class AmbassadorDashboardComponent implements OnInit {
  dashboard: AmbassadorDashboardResponse | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(private ambassadorPortalService: AmbassadorPortalService) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.ambassadorPortalService.getDashboard()
      .pipe(finalize(() => { this.isLoading = false; }))
      .subscribe({
        next: dashboard => {
          this.dashboard = dashboard;
        },
        error: error => {
          this.errorMessage = extractApiErrorMessage(error, 'Unable to load ambassador dashboard.');
        }
      });
  }

  get stats(): { label: string; value: string | number }[] {
    if (!this.dashboard) {
      return [];
    }

    return [
      { label: 'Assigned campaigns', value: this.dashboard.activeCampaigns },
      { label: 'Total clicks', value: this.dashboard.totalClicks },
      { label: 'Registrations', value: this.dashboard.totalRegistrations },
      { label: 'Completed rentals', value: this.dashboard.totalCompletedRentals },
      { label: 'Registration rate', value: `${this.dashboard.registrationConversionRate}%` }
    ];
  }
}
