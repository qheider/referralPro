import { Routes } from '@angular/router';
import { AmbassadorAnalyticsComponent } from './ambassador-analytics.component';
import { AmbassadorCampaignDetailComponent } from './ambassador-campaign-detail.component';
import { AmbassadorCampaignsComponent } from './ambassador-campaigns.component';
import { AmbassadorDashboardComponent } from './ambassador-dashboard.component';
import { AmbassadorEarningsComponent } from './ambassador-earnings.component';
import { AmbassadorLayoutComponent } from './ambassador-layout.component';
import { AmbassadorProfileComponent } from './ambassador-profile.component';
import { AmbassadorReferralsComponent } from './ambassador-referrals.component';

export const ambassadorRoutes: Routes = [
  {
    path: '',
    component: AmbassadorLayoutComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview'
      },
      {
        path: 'overview',
        component: AmbassadorDashboardComponent
      },
      {
        path: 'campaigns',
        component: AmbassadorCampaignsComponent
      },
      {
        path: 'campaigns/:campaignId',
        component: AmbassadorCampaignDetailComponent
      },
      {
        path: 'referrals',
        component: AmbassadorReferralsComponent
      },
      {
        path: 'analytics',
        component: AmbassadorAnalyticsComponent
      },
      {
        path: 'earnings',
        component: AmbassadorEarningsComponent
      },
      {
        path: 'profile',
        component: AmbassadorProfileComponent
      }
    ]
  }
];
