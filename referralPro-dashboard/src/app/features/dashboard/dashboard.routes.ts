import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { DashboardLayoutComponent } from './dashboard-layout.component';

export const dashboardRoutes: Routes = [
  {
    path: '',
    component: DashboardLayoutComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview'
      },
      {
        path: 'overview',
        component: DashboardComponent
      }
    ]
  }
];
