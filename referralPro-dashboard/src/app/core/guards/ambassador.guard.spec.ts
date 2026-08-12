import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ambassadorGuard } from './ambassador.guard';
import { AuthService } from '../services/auth.service';

describe('ambassadorGuard', () => {
  it('allows ambassadors', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { hasRole: () => true, getDefaultRoute: () => '/dashboard' } },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } }
      ]
    });

    const result = TestBed.runInInjectionContext(() => ambassadorGuard({} as never, {} as never));
    expect(result).toBeTrue();
  });
});
