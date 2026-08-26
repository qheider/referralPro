import { Routes } from '@angular/router';
import { ambassadorGuard } from './core/guards/ambassador.guard';
import { authGuard } from './core/guards/auth.guard';
import { companyAdminGuard } from './core/guards/company-admin.guard';
import { AcceptInvitationComponent } from './features/auth/accept-invitation.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password.component';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { ResetPasswordComponent } from './features/auth/reset-password.component';
import { VerifyEmailComponent } from './features/auth/verify-email.component';
import { CampaignJoinComponent } from './features/public/campaign-join.component';
import { CampaignReferComponent } from './features/public/campaign-refer.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'verify-email',
    component: VerifyEmailComponent
  },
  {
    // Public - lands here from the invitation email (EmailService.sendAmbassadorInvitationEmail)
    // with ?token=... to set a password and activate the account. Was previously unroutable and
    // fell through to the '**' wildcard below, bouncing invitees to /dashboard -> /login instead.
    path: 'accept-invitation',
    component: AcceptInvitationComponent
  },
  {
    // Public - lands here from the login page's "Forgot password?" link. Collects an email and
    // always shows a generic "check your email" panel - see PasswordResetController.
    path: 'forgot-password',
    component: ForgotPasswordComponent
  },
  {
    // Public - lands here from the reset-password email (EmailService.sendPasswordResetEmail)
    // with ?token=... to set a new password.
    path: 'reset-password',
    component: ResetPasswordComponent
  },
  {
    // Public campaign join link (Phase 3): /join/{campaignCode}
    path: 'join/:campaignCode',
    component: CampaignJoinComponent
  },
  {
    // Public end-user registration page a referred visitor lands on after clicking an
    // ambassador's /r/{token} link (Phase 4) - see ReferralClickService's redirect default.
    path: 'refer/:token',
    component: CampaignReferComponent
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.dashboardRoutes),
    canActivate: [authGuard, companyAdminGuard]
  },
  {
    path: 'ambassador',
    loadChildren: () => import('./features/ambassador/ambassador.routes').then(m => m.ambassadorRoutes),
    canActivate: [authGuard, ambassadorGuard]
  },
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
