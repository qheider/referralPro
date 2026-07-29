import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AmbassadorPortalService } from './ambassador-portal.service';

describe('AmbassadorPortalService', () => {
  let service: AmbassadorPortalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(AmbassadorPortalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads the ambassador dashboard', () => {
    let responseBody: unknown;

    service.getDashboard().subscribe(response => {
      responseBody = response;
    });

    const request = httpMock.expectOne(`${environment.apiUrl}/ambassador/dashboard`);
    expect(request.request.method).toBe('GET');

    request.flush({
      success: true,
      message: 'ok',
      data: {
        ambassadorId: 7,
        displayName: 'Jamie',
        activeCampaigns: 2,
        totalClicks: 10,
        totalRegistrations: 4,
        totalBookingsStarted: 2,
        totalCompletedRentals: 1,
        registrationConversionRate: 40,
        rentalConversionRate: 25,
        recentReferrals: []
      }
    });

    expect(responseBody).toEqual(jasmine.objectContaining({
      ambassadorId: 7,
      displayName: 'Jamie'
    }));
  });
});
