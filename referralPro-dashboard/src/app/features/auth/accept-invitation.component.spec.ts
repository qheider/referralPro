import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AcceptInvitationComponent } from './accept-invitation.component';
import { AuthService } from '../../core/services/auth.service';

describe('AcceptInvitationComponent', () => {
  const auth = {
    getInvitationDetails: () => of({ name: 'Jane Smith', email: 'jane@example.com' }),
    acceptInvitation: () => of({})
  };

  beforeEach(() => {
    auth.getInvitationDetails = () => of({ name: 'Jane Smith', email: 'jane@example.com' });
    TestBed.configureTestingModule({
      imports: [AcceptInvitationComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: { token: 'invitation-token' } } } },
        { provide: AuthService, useValue: auth }
      ]
    });
  });

  it('shows the invited identity as read-only fields beside the password form', () => {
    const fixture = TestBed.createComponent(AcceptInvitationComponent);
    fixture.detectChanges();
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect(inputs[0].value).toBe('Jane Smith');
    expect(inputs[0].readOnly).toBe(true);
    expect(inputs[1].value).toBe('jane@example.com');
    expect(inputs[1].readOnly).toBe(true);
    expect(inputs[2].type).toBe('password');
    expect(inputs[3].type).toBe('password');
  });

  it('hides the password form when invitation lookup fails', () => {
    auth.getInvitationDetails = () => throwError(() => new Error('Invalid or expired invitation'));
    const fixture = TestBed.createComponent(AcceptInvitationComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Invalid or expired invitation');
  });
});
