import { Routes } from '@angular/router';
import { ArcElement, BarController, BarElement, CategoryScale, DoughnutController, Legend, LineController, LineElement, LinearScale, PointElement, Tooltip } from 'chart.js';
import { provideCharts } from 'ng2-charts';
import { AmbassadorDetailComponent } from './ambassador-detail.component';
import { AmbassadorFormComponent } from './ambassador-form.component';
import { AmbassadorsComponent } from './ambassadors.component';
import { CampaignDetailComponent } from './campaign-detail.component';
import { CampaignFormComponent } from './campaign-form.component';
import { CampaignRevenueReportComponent } from './campaign-revenue-report.component';
import { DashboardComponent } from './dashboard.component';
import { DashboardLayoutComponent } from './dashboard-layout.component';
import { IntegrationSettingsComponent } from './integration-settings.component';
import { IntegrationSubmissionDetailComponent } from './integration-submission-detail.component';
import { IntegrationSubmissionsComponent } from './integration-submissions.component';
import { IntegrationWebhookEventDetailComponent } from './integration-webhook-event-detail.component';
import { IntegrationWebhookEventsComponent } from './integration-webhook-events.component';
import { RewardsComponent } from './rewards.component';

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
        path: 'campaigns/new',
        component: CampaignFormComponent
      },
      {
        path: 'campaigns/:campaignId',
        component: CampaignDetailComponent
      },
      {
        path: 'campaigns/:campaignId/revenue-report',
        component: CampaignRevenueReportComponent
      },
      {
        path: 'rewards',
        component: RewardsComponent
      },
      {
        path: 'ambassadors',
        component: AmbassadorsComponent
      },
      {
        path: 'ambassadors/new',
        component: AmbassadorFormComponent
      },
      {
        path: 'ambassadors/:ambassadorId',
        component: AmbassadorDetailComponent
      },
      {
        path: 'ambassadors/:ambassadorId/edit',
        component: AmbassadorFormComponent
      },
      {
        path: 'integration',
        component: IntegrationSettingsComponent
      },
      {
        path: 'integration/submissions',
        component: IntegrationSubmissionsComponent
      },
      {
        path: 'integration/submissions/:submissionId',
        component: IntegrationSubmissionDetailComponent
      },
      {
        path: 'integration/webhook-events',
        component: IntegrationWebhookEventsComponent
      },
      {
        path: 'integration/webhook-events/:webhookEventId',
        component: IntegrationWebhookEventDetailComponent
      }
    ]
  }
];
