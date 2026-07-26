# ReferralPro Dashboard

Angular dashboard application for the ReferralPro referral marketing platform.

## Overview

This dashboard allows company administrators to:
- View referral campaign analytics and metrics
- Monitor conversion funnels and performance
- Track top referrers and rewards
- Analyze time-series data for campaigns

## Tech Stack

- **Angular 21.2**: Latest Angular with standalone components
- **Tailwind CSS**: Utility-first CSS framework for styling
- **Chart.js + ng2-charts**: Data visualization for analytics charts
- **date-fns**: Modern date manipulation library
- **TypeScript**: Type-safe development

## Project Structure

```
src/
├── app/
│   ├── core/               # Core functionality (singleton services, guards, interceptors)
│   │   ├── services/       # Auth, HTTP services
│   │   ├── guards/         # Route guards (auth guard)
│   │   └── interceptors/   # HTTP interceptors (JWT token injection)
│   ├── features/           # Feature modules
│   │   ├── auth/          # Login, authentication components
│   │   └── dashboard/     # Dashboard pages (overview, campaign details)
│   ├── shared/            # Shared components and models
│   │   ├── components/    # Reusable UI components
│   │   └── models/        # TypeScript interfaces and types
│   ├── app.ts             # Root component
│   ├── app.config.ts      # App configuration
│   └── app.routes.ts      # Route configuration
├── environments/
│   ├── environment.ts     # Development environment config
│   └── environment.prod.ts # Production environment config
└── styles.css             # Global styles with Tailwind directives
```

## Development Setup

### Prerequisites

- Node.js 18+ and npm
- Angular CLI (`npm install -g @angular/cli`)
- Backend API running on `http://localhost:8080`

### Installation

```bash
cd referralPro-dashboard
npm install
```

### Development Server

```bash
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any source files.

### Build

```bash
# Development build
ng build

# Production build
ng build --configuration production
```

## Environment Configuration

### Development (`src/environments/environment.ts`)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### Production (`src/environments/environment.prod.ts`)
Update the `apiUrl` with your production backend URL before deploying.

## Backend API Integration

The dashboard connects to the ReferralPro Spring Boot backend API:

### Authentication Endpoints
- `POST /api/auth/login` - Login with username/password, returns JWT token
- `GET /api/auth/me` - Get current authenticated user details

### Dashboard Endpoints (JWT Required)
- `GET /api/dashboard/campaigns/overview` - Get all campaigns overview
- `GET /api/dashboard/campaigns/{id}/stats` - Get campaign statistics
- `GET /api/dashboard/campaigns/{id}/funnel` - Get conversion funnel data
- `GET /api/dashboard/campaigns/{id}/top-referrers` - Get top referrers leaderboard
- `GET /api/dashboard/campaigns/{id}/metrics/time-series` - Get time-series metrics
- `GET /api/dashboard/campaigns/{id}/rewards/summary` - Get rewards summary

### Test Credentials
- **Username**: `admin@company.com`
- **Password**: `password123`
- **Company**: ABC Cleaning (ID: 1)

## Next Steps

### Phase 4: Authentication Module
- Create login component with reactive forms
- Implement AuthService with JWT token management
- Add HTTP interceptor for Bearer token injection
- Create auth guard for protected routes
- Style login page with Tailwind

### Phase 5: Dashboard Layout
- Main layout component with sidebar navigation
- Header with user info and logout button
- Responsive design with Tailwind
- Route configuration

### Phase 6: Dashboard Overview
- Campaign list component
- Metrics cards
- Chart.js visualizations
- Refresh and filter controls

### Phase 7: Campaign Detail Page
- Detailed campaign statistics
- Conversion funnel visualization
- Top referrers leaderboard
- Time-series graphs
- Reward summary

### Phase 8: Polish & Testing
- Error handling
- Loading states
- Empty states
- Form validation
- Responsive testing
- E2E testing

## Available Scripts

- `ng serve` - Start development server
- `ng build` - Build the project
- `ng test` - Run unit tests
- `ng lint` - Run linting
- `npm run build:prod` - Production build

## Notes

- The app uses standalone components (no NgModules)
- CORS is configured on the backend for `http://localhost:4200`
- JWT tokens expire after 24 hours
- Multi-tenancy is handled automatically by the backend based on JWT company ID
