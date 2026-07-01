import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(DashboardService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should unwrap the campaign overview response payload', () => {
    let result: unknown;

    service.getCampaignsOverview().subscribe(response => {
      result = response;
    });

    const request = httpTestingController.expectOne('http://localhost:8080/api/dashboard/campaigns/overview');
    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      message: 'Campaigns overview retrieved successfully',
      data: {
        companyId: 1,
        companyName: 'ABC Cleaning',
        campaigns: []
      }
    });

    expect(result).toEqual({
      companyId: 1,
      companyName: 'ABC Cleaning',
      campaigns: []
    });
  });

  it('should call the campaign stats endpoint for a specific campaign', () => {
    let result: unknown;

    service.getCampaignStats(42).subscribe(response => {
      result = response;
    });

    const request = httpTestingController.expectOne('http://localhost:8080/api/dashboard/campaigns/42/stats');
    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      message: 'Campaign stats retrieved successfully',
      data: {
        campaignId: 42,
        campaignName: 'Spring promo',
        status: 'ACTIVE',
        totalReferrals: 100,
        totalClicks: 80,
        totalConversions: 25,
        clickThroughRate: 80,
        conversionRate: 31.25,
        totalRewardsIssued: 50,
        totalRewardValue: 500,
        rewardsRedeemed: 20,
        averageRewardValue: 10
      }
    });

    expect(result).toEqual({
      campaignId: 42,
      campaignName: 'Spring promo',
      status: 'ACTIVE',
      totalReferrals: 100,
      totalClicks: 80,
      totalConversions: 25,
      clickThroughRate: 80,
      conversionRate: 31.25,
      totalRewardsIssued: 50,
      totalRewardValue: 500,
      rewardsRedeemed: 20,
      averageRewardValue: 10
    });
  });
});
