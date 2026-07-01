import { Routes } from '@angular/router';
import { ArcElement, BarController, BarElement, CategoryScale, DoughnutController, Legend, LineController, LineElement, LinearScale, PointElement, Tooltip } from 'chart.js';
import { provideCharts } from 'ng2-charts';
import { CampaignDetailComponent } from './campaign-detail.component';
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
          LineController,
          LineElement,
          PointElement,
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
      },
      {
        path: 'campaigns/:campaignId',
        component: CampaignDetailComponent
      }
    ]
  }
];
