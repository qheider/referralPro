import { Routes } from '@angular/router';
import { ambassadorGuard } from './core/guards/ambassador.guard';
import { authGuard } from './core/guards/auth.guard';
import { companyAdminGuard } from './core/guards/company-admin.guard';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { CampaignJoinComponent } from './features/public/campaign-join.component';

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
    // Public campaign join link (Phase 3): /join/{campaignCode}
    path: 'join/:campaignCode',
    component: CampaignJoinComponent
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
