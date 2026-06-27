import { Routes } from '@angular/router';
import { ArcElement, BarController, BarElement, CategoryScale, DoughnutController, Legend, LinearScale, Tooltip } from 'chart.js';
import { provideCharts } from 'ng2-charts';
import { DashboardComponent } from './dashboard.component';
import { DashboardLayoutComponent } from './dashboard-layout.component';

export const dashboardRoutes: Routes = [
  {
    path: '',
    component: DashboardLayoutComponent,
    providers: [
      provideCharts({
        registerables: [
          BarController,
          BarElement,
          DoughnutController,
          ArcElement,
          CategoryScale,
          LinearScale,
          Legend,
          Tooltip
        ]
      })
    ],
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
