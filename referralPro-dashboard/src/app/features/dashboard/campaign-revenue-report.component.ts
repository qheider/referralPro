import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { RevenueService } from '../../core/services/revenue.service';
import { CampaignRevenueReportResponse } from '../../shared/models/revenue.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-campaign-revenue-report',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './campaign-revenue-report.component.html',
  styleUrl: './campaign-revenue-report.component.css'
})
export class CampaignRevenueReportComponent implements OnInit {
  campaignId: number | null = null;
  report: CampaignRevenueReportResponse | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(private route: ActivatedRoute, private revenueService: RevenueService) {}

  ngOnInit(): void {
    const campaignId = Number(this.route.snapshot.paramMap.get('campaignId'));
    if (!Number.isInteger(campaignId) || campaignId <= 0) {
      this.errorMessage = 'Invalid campaign.';
      return;
    }
    this.campaignId = campaignId;
    this.load();
  }

  load(): void {
    if (!this.campaignId) {
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.revenueService
      .getCampaignReport(this.campaignId)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: report => (this.report = report),
        error: (error: unknown) => (this.errorMessage = extractApiErrorMessage(error, 'Unable to load revenue report.'))
      });
  }

  currencyEntries(): { currency: string; amount: number }[] {
    if (!this.report) {
      return [];
    }
    return Object.entries(this.report.revenueByCurrency).map(([currency, amount]) => ({ currency, amount }));
  }
}
