import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AmbassadorAdminService } from './ambassador-admin.service';

describe('AmbassadorAdminService', () => {
  let service: AmbassadorAdminService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AmbassadorAdminService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AmbassadorAdminService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should list ambassadors with filters', () => {
    let result: unknown;

    service.listAmbassadors({ page: 1, search: 'sarah', status: 'ACTIVE' }).subscribe(response => {
      result = response;
    });

    const request = httpTestingController.expectOne(req =>
      req.url === 'http://localhost:8080/api/admin/ambassadors'
      && req.params.get('page') === '1'
      && req.params.get('size') === '20'
      && req.params.get('search') === 'sarah'
      && req.params.get('status') === 'ACTIVE'
    );

    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      data: {
        content: [],
        page: 1,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        first: false,
        last: true
      }
    });

    expect(result).toEqual({
      content: [],
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true
    });
  });

  it('should post a new ambassador payload', () => {
    service.createAmbassador({
      firstName: 'Sarah',
      lastName: 'Ahmed',
      email: 'sarah@example.com'
    }).subscribe();

    const request = httpTestingController.expectOne('http://localhost:8080/api/admin/ambassadors');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      firstName: 'Sarah',
      lastName: 'Ahmed',
      email: 'sarah@example.com'
    });
    request.flush({
      success: true,
      message: 'Ambassador created successfully',
      data: {}
    });
  });
});
