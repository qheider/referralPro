import { CommonModule } from '@angular/common';
import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { forkJoin, finalize } from 'rxjs';
import { DashboardService } from '../../core/services/dashboard.service';
import {
  CampaignStatsResponse,
  ConversionFunnelResponse,
  RewardSummaryResponse,
  TimeSeriesResponse,
  TopReferrersResponse
} from '../../shared/models/dashboard.model';
import { extractApiErrorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-campaign-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective],
  templateUrl: './campaign-detail.component.html',
  styleUrl: './campaign-detail.component.css'
})
export class CampaignDetailComponent implements OnInit {
  campaignId: number | null = null;
  stats: CampaignStatsResponse | null = null;
  funnel: ConversionFunnelResponse | null = null;
  topReferrers: TopReferrersResponse | null = null;
  timeSeries: TimeSeriesResponse | null = null;
  rewardSummary: RewardSummaryResponse | null = null;
  isLoading = false;
  errorMessage = '';

  readonly funnelChartType = 'bar';
  readonly timeSeriesChartType = 'line';
  readonly rewardsChartType = 'doughnut';

  readonly funnelChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      }
    },
    scales: {
      y: {
        beginAtZero: true
      }
    }
  };

  readonly timeSeriesChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false
    },
    plugins: {
      legend: {
        position: 'bottom'
      }
    },
    scales: {
      y: {
        beginAtZero: true
      }
    }
  };

  readonly rewardsChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      }
    }
  };

  funnelChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: []
  };

  timeSeriesChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: []
  };

  rewardsChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: [],
    datasets: []
  };

  constructor(
    private route: ActivatedRoute,
    private dashboardService: DashboardService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const campaignId = Number(this.route.snapshot.paramMap.get('campaignId'));

    if (!Number.isInteger(campaignId) || campaignId <= 0) {
      this.errorMessage = 'Invalid campaign id.';
      return;
    }

    this.campaignId = campaignId;
    this.loadCampaignDetail(campaignId);
  }

  get pageTitle(): string {
    return this.stats?.campaignName || this.funnel?.campaignName || `Campaign #${this.campaignId ?? ''}`;
  }

  get hasData(): boolean {
    return !!(this.stats && this.funnel && this.topReferrers && this.timeSeries && this.rewardSummary);
  }

  get topReferrerRows() {
    return this.topReferrers?.topReferrers ?? [];
  }

  get hasTopReferrers(): boolean {
    return this.topReferrerRows.length > 0;
  }

  get hasRewardBreakdown(): boolean {
    return (this.rewardSummary?.breakdownByType.length ?? 0) > 0;
  }

  get hasTimeSeriesData(): boolean {
    return (this.timeSeries?.dataPoints.length ?? 0) > 0;
  }

  refresh(): void {
    if (this.campaignId) {
      this.loadCampaignDetail(this.campaignId);
    }
  }

  private loadCampaignDetail(campaignId: number): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.ngZone.run(() => {
      forkJoin({
        stats: this.dashboardService.getCampaignStats(campaignId),
        funnel: this.dashboardService.getConversionFunnel(campaignId),
        topReferrers: this.dashboardService.getTopReferrers(campaignId),
        timeSeries: this.dashboardService.getTimeSeries(campaignId),
        rewardSummary: this.dashboardService.getRewardSummary(campaignId)
      })
        .pipe(finalize(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
        }))
        .subscribe({
          next: result => {
            this.stats = result.stats;
            this.funnel = result.funnel;
            this.topReferrers = result.topReferrers;
            this.timeSeries = result.timeSeries;
            this.rewardSummary = result.rewardSummary;
            this.updateCharts();
            this.cdr.detectChanges();
          },
          error: error => {
            console.error('Campaign detail failed:', error);
            this.errorMessage = extractApiErrorMessage(error, 'Unable to load campaign detail.');
            this.stats = null;
            this.funnel = null;
            this.topReferrers = null;
            this.timeSeries = null;
            this.rewardSummary = null;
            this.updateCharts();
          }
        });
    });
  }

  private updateCharts(): void {
    this.funnelChartData = {
      labels: this.funnel ? ['Referrals', 'Clicks', 'Conversions'] : [],
      datasets: this.funnel
        ? [
            {
              data: [
                this.funnel.referralsCount,
                this.funnel.clicksCount,
                this.funnel.conversionsCount
              ],
              backgroundColor: ['#6366f1', '#0ea5e9', '#10b981']
            }
          ]
        : []
    };

    this.timeSeriesChartData = {
      labels: this.timeSeries?.dataPoints.map(point => point.date) ?? [],
      datasets: this.timeSeries
        ? [
            {
              label: 'Referrals',
              data: this.timeSeries.dataPoints.map(point => point.referrals),
              borderColor: '#6366f1',
              backgroundColor: 'rgba(99, 102, 241, 0.15)',
              tension: 0.35
            },
            {
              label: 'Conversions',
              data: this.timeSeries.dataPoints.map(point => point.conversions),
              borderColor: '#10b981',
              backgroundColor: 'rgba(16, 185, 129, 0.15)',
              tension: 0.35
            },
            {
              label: 'Rewards',
              data: this.timeSeries.dataPoints.map(point => point.rewards),
              borderColor: '#f59e0b',
              backgroundColor: 'rgba(245, 158, 11, 0.15)',
              tension: 0.35
            }
          ]
        : []
    };

    this.rewardsChartData = {
      labels: this.rewardSummary?.breakdownByType.map(item => item.rewardType) ?? [],
      datasets: this.rewardSummary
        ? [
            {
              data: this.rewardSummary.breakdownByType.map(item => item.count),
              backgroundColor: ['#6366f1', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6']
            }
          ]
        : []
    };
  }
}
