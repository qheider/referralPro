import { Routes } from '@angular/router';
import { ambassadorGuard } from './core/guards/ambassador.guard';
import { authGuard } from './core/guards/auth.guard';
import { companyAdminGuard } from './core/guards/company-admin.guard';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';

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
