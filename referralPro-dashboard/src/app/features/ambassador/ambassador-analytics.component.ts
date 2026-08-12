import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AmbassadorPortalService } from '../../core/services/ambassador-portal.service';
import { AmbassadorAnalyticsResponse } from '../../shared/models/ambassador-portal.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-ambassador-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6">
      <div>
        <h2 class="text-2xl font-semibold">Performance analytics</h2>
        <p class="text-sm text-slate-400">Last {{ analytics ? analytics.trends.length : 0 }} days of click and conversion performance.</p>
      </div>

      <p *ngIf="errorMessage" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ errorMessage }}</p>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4" *ngIf="analytics">
        <div class="rounded-xl border border-slate-800 bg-slate-950 p-4" *ngFor="let stat of stats">
          <p class="text-sm text-slate-400">{{ stat.label }}</p>
          <p class="mt-2 text-2xl font-semibold">{{ stat.value }}</p>
        </div>
      </div>

      <section class="rounded-xl border border-slate-800" *ngIf="analytics">
        <div class="grid grid-cols-[1.5fr_1fr_1fr_1fr_1fr_1fr] gap-4 px-4 py-3 text-xs uppercase tracking-wide text-slate-500">
          <span>Campaign</span>
          <span>Clicks</span>
          <span>Registrations</span>
          <span>Rentals</span>
          <span>Reg. rate</span>
          <span>Rental rate</span>
        </div>
        <div *ngFor="let campaign of analytics.campaigns" class="grid grid-cols-[1.5fr_1fr_1fr_1fr_1fr_1fr] gap-4 border-t border-slate-800 px-4 py-3 text-sm">
          <span>{{ campaign.campaignName }}</span>
          <span>{{ campaign.clicks }}</span>
          <span>{{ campaign.registrations }}</span>
          <span>{{ campaign.completedRentals }}</span>
          <span>{{ campaign.registrationConversionRate }}%</span>
          <span>{{ campaign.rentalConversionRate }}%</span>
        </div>
      </section>
    </div>
  `
})
export class AmbassadorAnalyticsComponent implements OnInit {
  analytics: AmbassadorAnalyticsResponse | null = null;
  errorMessage = '';

  constructor(private ambassadorPortalService: AmbassadorPortalService) {}

  ngOnInit(): void {
    this.ambassadorPortalService.getAnalytics({}).subscribe({
      next: analytics => {
        this.analytics = analytics;
      },
      error: error => {
        this.errorMessage = extractApiErrorMessage(error, 'Unable to load analytics.');
      }
    });
  }

  get stats(): { label: string; value: string | number }[] {
    if (!this.analytics) {
      return [];
    }

    return [
      { label: 'Clicks', value: this.analytics.totalClicks },
      { label: 'Registrations', value: this.analytics.totalRegistrations },
      { label: 'Bookings started', value: this.analytics.totalBookingsStarted },
      { label: 'Completed rentals', value: this.analytics.totalCompletedRentals },
      { label: 'Registration rate', value: `${this.analytics.registrationConversionRate}%` },
      { label: 'Rental rate', value: `${this.analytics.rentalConversionRate}%` }
    ];
  }
}
