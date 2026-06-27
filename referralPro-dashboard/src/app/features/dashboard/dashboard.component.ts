import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../shared/models/auth.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { CampaignOverviewItem, CampaignsOverviewResponse } from '../../shared/models/dashboard.model';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  overview: CampaignsOverviewResponse | null = null;
  selectedStatus = 'ALL';
  isLoading = false;
  errorMessage = '';
  lastUpdated: Date | null = null;

  readonly performanceChartType = 'bar';
  readonly statusChartType = 'doughnut';
  readonly performanceChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
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
  readonly statusChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      }
    }
  };

  performanceChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: []
  };
  statusChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: [],
    datasets: []
  };

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUserValue();

    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.loadOverview();
  }

  get availableStatuses(): string[] {
    const statuses = new Set(this.overview?.campaigns.map(campaign => campaign.status) ?? []);
    return ['ALL', ...Array.from(statuses)];
  }

  get filteredCampaigns(): CampaignOverviewItem[] {
    const campaigns = this.overview?.campaigns ?? [];
    if (this.selectedStatus === 'ALL') {
      return campaigns;
    }

    return campaigns.filter(campaign => campaign.status === this.selectedStatus);
  }

  get totalCampaigns(): number {
    return this.filteredCampaigns.length;
  }

  get totalReferrals(): number {
    return this.filteredCampaigns.reduce((sum, campaign) => sum + campaign.referralCount, 0);
  }

  get totalClicks(): number {
    return this.filteredCampaigns.reduce((sum, campaign) => sum + campaign.clickCount, 0);
  }

  get totalConversions(): number {
    return this.filteredCampaigns.reduce((sum, campaign) => sum + campaign.conversionCount, 0);
  }

  get averageConversionRate(): number {
    if (!this.filteredCampaigns.length) {
      return 0;
    }

    const totalRate = this.filteredCampaigns.reduce((sum, campaign) => sum + campaign.conversionRate, 0);
    return totalRate / this.filteredCampaigns.length;
  }

  get hasCampaigns(): boolean {
    return this.filteredCampaigns.length > 0;
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = status;
    this.updateCharts();
  }

  refreshOverview(): void {
    this.loadOverview();
  }

  trackByCampaignId(_index: number, campaign: CampaignOverviewItem): number {
    return campaign.campaignId;
  }

  private loadOverview(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.dashboardService
      .getCampaignsOverview()
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: overview => {
          this.overview = overview;
          this.lastUpdated = new Date();

          if (!this.availableStatuses.includes(this.selectedStatus)) {
            this.selectedStatus = 'ALL';
          }

          this.updateCharts();
        },
        error: error => {
          console.error('Dashboard overview failed:', error);
          this.errorMessage = error.error?.message || error.message || 'Unable to load dashboard overview.';
          this.overview = null;
          this.updateCharts();
        }
      });
  }

  private updateCharts(): void {
    const campaigns = [...this.filteredCampaigns]
      .sort((left, right) => right.referralCount - left.referralCount)
      .slice(0, 5);

    this.performanceChartData = {
      labels: campaigns.map(campaign => campaign.campaignName),
      datasets: [
        {
          label: 'Referrals',
          data: campaigns.map(campaign => campaign.referralCount),
          backgroundColor: '#6366f1'
        },
        {
          label: 'Conversions',
          data: campaigns.map(campaign => campaign.conversionCount),
          backgroundColor: '#10b981'
        }
      ]
    };

    const statusTotals = this.filteredCampaigns.reduce<Record<string, number>>((totals, campaign) => {
      totals[campaign.status] = (totals[campaign.status] || 0) + 1;
      return totals;
    }, {});

    this.statusChartData = {
      labels: Object.keys(statusTotals),
      datasets: [
        {
          data: Object.values(statusTotals),
          backgroundColor: ['#6366f1', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6']
        }
      ]
    };
  }
}
