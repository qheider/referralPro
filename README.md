# ReferralPro

A comprehensive multi-tenant referral marketing platform with a Spring Boot backend API and an Angular web dashboard.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Quick Start](#quick-start-both-backend--dashboard)
  - [Application URLs](#application-urls)
  - [Backend Setup](#backend-setup-spring-boot-api)
  - [Dashboard Setup](#dashboard-setup-angular-frontend)
- [API Testing Flow](#api-testing-flow)
- [Dashboard Features & Endpoints](#dashboard-features--endpoints)
- [Database Schema](#database-schema)
- [Useful Commands](#useful-commands)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Validation Rules](#validation-rules)
- [Security](#security)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Development Tips](#development-tips)
- [API Documentation](#api-documentation)
- [Future Enhancements](#future-enhancements)
- [License](#license)
- [Support](#support)

## Overview

ReferralPro consists of two main components:

1. **Backend API** - Built with Spring Boot 3.x and Java 21, providing RESTful endpoints for referral management, conversion tracking, and reward issuance
2. **Web Dashboard** - Built with Angular 21, offering an intuitive interface for managing campaigns, viewing analytics, and monitoring referral performance

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        ReferralPro Platform                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐              ┌──────────────────┐        │
│  │  Angular 21      │              │   Spring Boot    │        │
│  │  Dashboard       │◄────────────►│   Backend API    │        │
│  │  (Port 4200)     │   HTTP/JWT   │   (Port 8080)    │        │
│  └──────────────────┘              └─────────┬────────┘        │
│         │                                     │                  │
│         │                          ┌──────────▼────────┐        │
│         │                          │    MySQL 8.4      │        │
│         │                          │   Database        │        │
│         │                          │   (Port 3306)     │        │
│         │                          └───────────────────┘        │
│         │                                                        │
│         └────► Authentication Flow:                             │
│                - Dashboard: JWT (Bearer Token)                  │
│                - Integration: API Key                           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

External Integration:
  Company Backend ──API Key──► Backend API (/api/referrals, /api/conversions)
  End Users ──────────────────► Public Link (/r/{code}) ──► Track & Redirect
```

## Features

- **Multi-tenant architecture**: Support multiple companies with isolated data
- **Dual authentication**: API key auth for backend integration + JWT auth for dashboard users
- **Campaign management**: Create and manage referral campaigns with custom rewards
- **Referral link generation**: Generate unique shareable referral links
- **Click tracking**: Track referral link clicks with IP and user agent
- **Conversion tracking**: Validate and track successful referrals
- **Automatic reward issuance**: Issue rewards to both referrer and referee
- **Analytics dashboard**: Real-time metrics, funnels, leaderboards, and time-series data
- **User management**: Dashboard users with secure authentication

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.x
- Spring Data JPA
- Spring Security (API Key + JWT)
- MySQL 8.4
- Flyway (database migrations)
- Lombok
- Maven
- OpenAPI/Swagger UI

### Frontend
- Angular 21
- TypeScript
- Tailwind CSS
- Standalone Components
- RxJS
- Chart.js (for analytics)

### Infrastructure
- Docker & Docker Compose

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Node.js 18+ and npm 9+
- Docker and Docker Compose

## Getting Started

This guide will help you set up and run both the backend API and the frontend dashboard.

### Quick Start (Both Backend & Dashboard)

1. **Start MySQL Database**
   ```bash
   docker-compose up -d
   ```

2. **Start the Backend API**
   ```bash
   # On Windows
   .\mvnw.cmd clean install
   .\mvnw.cmd spring-boot:run
   
   # On Linux/Mac
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```
   
   Backend will be available at `http://localhost:8080`

3. **Start the Dashboard**
   ```bash
   cd referralPro-dashboard
   npm install
   npm start
   ```
   
   Dashboard will be available at `http://localhost:4200`

### Application URLs

Once both applications are running, you can access:

| Service | URL | Purpose |
|---------|-----|---------|
| **Backend API** | `http://localhost:8080` | REST API endpoints |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | API documentation & testing |
| **Dashboard** | `http://localhost:4200` | Web interface |
| **Dashboard Login** | `http://localhost:4200/login` | Login page |
| **MySQL** | `localhost:3306` | Database (internal) |

### Test Credentials

**Dashboard Users:**
- Admin: `admin@demo.com` / `password123`
- Manager: `manager@demo.com` / `password123`
- Viewer: `viewer@demo.com` / `password123`

**API Key:** Obtain by registering a company via `POST /api/companies/register`

---

## Backend Setup (Spring Boot API)

### 1. Start MySQL Database

```bash
docker-compose up -d
```

This will start a MySQL 8.4 container on port 3306 with the following credentials:
- Database: `referral_platform`
- Username: `root`
- Password: `rootpassword`

### 2. Build the Backend

```bash
# On Windows
.\mvnw.cmd clean install

# On Linux/Mac
./mvnw clean install
```

### 3. Run the Backend

```bash
# On Windows
.\mvnw.cmd spring-boot:run

# On Linux/Mac
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

### 4. Run Backend Tests

```bash
# Run all tests
.\mvnw.cmd test

# Run a specific test class
.\mvnw.cmd -Dtest=ReferralProApplicationTests test
```

### 5. Access Swagger UI

Open your browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```

---

## Dashboard Setup (Angular Frontend)

### 1. Install Dependencies

```bash
cd referralPro-dashboard
npm install
```

### 2. Configure Environment

The dashboard is configured to connect to the backend at `http://localhost:8080/api` by default. 

To modify this, edit:
- Development: `src/environments/environment.ts`
- Production: `src/environments/environment.prod.ts`

### 3. Start Development Server

```bash
npm start
```

The dashboard will be available at `http://localhost:4200` and will automatically reload when you make changes.

### 4. Build for Production

```bash
npm run build
```

Production build will be output to `dist/referral-pro-dashboard/`.

### 5. Run Dashboard Tests

```bash
# Run tests once
npm test -- --watch=false

# Run a specific test file
npm test -- --watch=false --include src/app/app.spec.ts

# Run tests in watch mode (default)
npm test
```

### 6. Login to Dashboard

After starting the dashboard:

1. Navigate to `http://localhost:4200/login`
2. Use one of the test dashboard user credentials:
   - **Admin User**: `admin@demo.com` / `password123`
   - **Manager User**: `manager@demo.com` / `password123`
   - **Viewer User**: `viewer@demo.com` / `password123`

---

## API Testing Flow

### Step 1: Register a Company

```bash
POST http://localhost:8080/api/companies/register
Content-Type: application/json

{
  "name": "ABC Cleaning",
  "email": "admin@abc-cleaning.com"
}
```

**Response**: Save the `apiKey` from the response. You'll need it for subsequent API calls.

### Step 2: Create a Campaign

```bash
POST http://localhost:8080/api/companies/1/campaigns
Authorization: ApiKey {your-api-key}
Content-Type: application/json

{
  "name": "Refer a Friend",
  "description": "Give $10, get $10",
  "landingPageUrl": "https://abc-cleaning.com/signup",
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-12-31T23:59:59",
  "rewardType": "DISCOUNT_AMOUNT",
  "referrerRewardValue": 10.00,
  "refereeRewardValue": 10.00,
  "conversionEventName": "SERVICE_COMPLETED"
}
```

### Step 3: Generate a Referral Link

```bash
POST http://localhost:8080/api/referrals/generate
Authorization: ApiKey {your-api-key}
Content-Type: application/json

{
  "campaignId": 1,
  "externalUserId": "cust_5001",
  "email": "john@test.com",
  "name": "John Smith"
}
```

**Response**: Save the `referralCode` from the response.

### Step 4: Simulate Friend Clicking Referral Link

```bash
GET http://localhost:8080/r/{referralCode}
```

This will:
- Record the click
- Redirect to the campaign landing page with `?ref={referralCode}` parameter

### Step 5: Complete a Conversion

When the referred friend completes the required service:

```bash
POST http://localhost:8080/api/conversions
Authorization: ApiKey {your-api-key}
Content-Type: application/json

{
  "referralCode": "{referralCode}",
  "externalUserId": "cust_9009",
  "email": "friend@test.com",
  "name": "Friend User",
  "eventName": "SERVICE_COMPLETED"
}
```

**Response**: Contains reward information for both referrer and referee.

### Step 6: Get User Rewards

```bash
GET http://localhost:8080/api/rewards/users/cust_5001
Authorization: ApiKey {your-api-key}
```

---

## Dashboard Features & Endpoints

The Angular dashboard provides a comprehensive interface for monitoring and managing your referral program.

### Dashboard Analytics Endpoints

All dashboard endpoints require JWT authentication (`Authorization: Bearer {token}`).

#### 1. Login
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@demo.com",
  "password": "password123"
}
```

#### 2. Get Current User
```bash
GET http://localhost:8080/api/auth/me
Authorization: Bearer {jwt-token}
```

#### 3. Dashboard Overview
```bash
GET http://localhost:8080/api/dashboard/overview
Authorization: Bearer {jwt-token}
```

Returns:
- Total referrals
- Total conversions
- Conversion rate
- Total revenue
- Active campaigns

#### 4. Referral Funnel
```bash
GET http://localhost:8080/api/dashboard/funnel
Authorization: Bearer {jwt-token}
```

Returns stage-by-stage breakdown:
- Referrals generated
- Clicks received
- Conversions completed
- Rewards issued

#### 5. Top Referrers Leaderboard
```bash
GET http://localhost:8080/api/dashboard/top-referrers?limit=10
Authorization: Bearer {jwt-token}
```

#### 6. Time Series Data
```bash
GET http://localhost:8080/api/dashboard/time-series?period=7d
Authorization: Bearer {jwt-token}
```

Periods: `7d`, `30d`, `90d`, `1y`

#### 7. Reward Summary
```bash
GET http://localhost:8080/api/dashboard/rewards-summary
Authorization: Bearer {jwt-token}
```

### Dashboard Pages

- **Login** (`/login`) - User authentication
- **Dashboard Home** (`/dashboard`) - Overview metrics and charts
- **Campaigns** - Campaign management (future enhancement)
- **Referrals** - Referral tracking and management (future enhancement)
- **Analytics** - Detailed analytics and reports (future enhancement)

---

## Database Schema

The application uses Flyway for database migrations. The schema includes:

- **companies**: Company accounts with API keys
- **campaigns**: Referral campaigns with reward configuration
- **platform_users**: End users (customers) of companies
- **referrals**: Generated referral links
- **referral_clicks**: Click tracking data
- **conversions**: Successful referral conversions
- **rewards**: Issued rewards (coupons/credits)
- **dashboard_users**: Admin/manager users for the dashboard (JWT authentication)

### Database Migrations

Migrations are located in `src/main/resources/db/migration/`:

- `V1__create_companies.sql` - Company accounts
- `V2__create_campaigns.sql` - Campaign management
- `V3__create_platform_users.sql` - End users
- `V4__create_referrals.sql` - Referral links
- `V5__create_referral_clicks.sql` - Click tracking
- `V6__create_conversions.sql` - Conversion events
- `V7__create_rewards.sql` - Reward records
- `V8__create_dashboard_users.sql` - Dashboard users
- `V9__add_missing_updated_at_columns.sql` - Audit timestamps
- `V10__insert_test_dashboard_users.sql` - Test dashboard user data

---

## Useful Commands

### Backend Commands

```bash
# Start MySQL
docker-compose up -d

# Stop MySQL
docker-compose down

# Compile backend (skip tests)
.\mvnw.cmd -DskipTests compile

# Full backend build
.\mvnw.cmd clean install

# Run backend
.\mvnw.cmd spring-boot:run

# Run all tests
.\mvnw.cmd test

# Run specific test class
.\mvnw.cmd -Dtest=ReferralProApplicationTests test

# Package as JAR
.\mvnw.cmd package
```

### Dashboard Commands

```bash
# Install dependencies
cd referralPro-dashboard && npm install

# Start dev server
npm start

# Build for production
npm run build

# Run tests once
npm test -- --watch=false

# Run tests in watch mode
npm test

# Run specific test
npm test -- --watch=false --include src/app/app.spec.ts

# Lint (if configured)
npm run lint
```

### Docker Commands

```bash
# View running containers
docker ps

# View container logs
docker-compose logs -f

# Restart MySQL
docker-compose restart

# Remove containers and volumes
docker-compose down -v

# Rebuild and restart
docker-compose up -d --build
```

---

## Configuration

### Backend Configuration

Main configuration is in `src/main/resources/application.yml`:

**Database Settings:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/referral_platform
    username: root
    password: rootpassword
```

**JPA/Hibernate:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
```

**Application Settings:**
```yaml
app:
  base-url: http://localhost:8080
  jwt:
    secret: your-secret-key-min-256-bits
    expiration: 86400000  # 24 hours
```

**Flyway:**
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
```

### Frontend Configuration

Dashboard configuration files in `src/environments/`:

**Development** (`environment.ts`):
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

**Production** (`environment.prod.ts`):
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://your-production-api.com/api'
};
```

## Project Structure

### Backend Structure
```
src/main/java/com/actpro/referral/
├── auth/             # JWT authentication for dashboard users
├── company/          # Company registration and management
├── campaign/         # Campaign CRUD operations
├── user/             # Platform user management
├── referral/         # Referral link generation and redirect
├── click/            # Click tracking
├── conversion/       # Conversion processing
├── reward/           # Reward issuance and lookup
├── dashboard/        # Dashboard analytics endpoints
├── security/         # API key + JWT authentication
├── common/           # Shared utilities and exceptions
└── config/           # Application configuration
```

### Frontend Structure
```
referralPro-dashboard/
├── src/
│   ├── app/
│   │   ├── core/              # Core services, guards, interceptors
│   │   │   ├── guards/        # Auth guard
│   │   │   ├── interceptors/  # HTTP interceptors (auth)
│   │   │   └── services/      # Auth & dashboard services
│   │   ├── features/          # Feature modules
│   │   │   ├── auth/          # Login component
│   │   │   └── dashboard/     # Dashboard components
│   │   └── shared/            # Shared components, models, utils
│   └── environments/          # Environment configurations
├── public/                    # Static assets
└── tailwind.config.js         # Tailwind CSS configuration
```

## Validation Rules

The platform enforces the following business rules:

- Company email must be unique
- Campaign end date must be after start date
- Only ACTIVE campaigns can generate conversions
- Self-referral is not allowed
- Duplicate conversions are prevented
- Event name must match campaign configuration
- API key isolation: companies can only access their own data

## Security

The platform implements two authentication mechanisms:

### 1. API Key Authentication
- **Purpose**: Secure integration for company backend systems
- **Usage**: Required for all integration endpoints
- **Header**: `Authorization: ApiKey {api-key}`
- **Endpoints**: 
  - `/api/referrals/**` - Referral generation
  - `/api/conversions/**` - Conversion tracking
  - `/api/rewards/**` - Reward lookup
  - `/api/companies/{companyId}/**` - Company management
- **Isolation**: API key determines which company's data is accessible

### 2. JWT Authentication
- **Purpose**: Dashboard user access
- **Usage**: Required for dashboard endpoints
- **Header**: `Authorization: Bearer {jwt-token}`
- **Endpoints**:
  - `/api/auth/**` - Login and user info
  - `/api/dashboard/**` - Analytics and metrics
- **Flow**: Login → Receive JWT → Include in subsequent requests

### Public Endpoints
- `GET /r/{referralCode}` - Referral link redirect (tracking)
- `POST /api/companies/register` - Company registration

### Company Isolation
- All authenticated requests are scoped to the authenticated company
- Cross-tenant data access is prevented
- Both API key and JWT auth populate the company context

## Testing

### Backend Tests

Run unit and integration tests:

```bash
# On Windows
.\mvnw.cmd test

# On Linux/Mac
./mvnw test

# Run specific test
.\mvnw.cmd -Dtest=ReferralProApplicationTests test
```

### Frontend Tests

```bash
cd referralPro-dashboard

# Run all tests once
npm test -- --watch=false

# Run tests in watch mode
npm test

# Run specific test file
npm test -- --watch=false --include src/app/app.spec.ts
```

---

## Troubleshooting

### Backend Issues

**Port 8080 already in use:**
```bash
# Find and kill the process using port 8080
# Windows
netstat -ano | findstr :8080
taskkill /PID <process-id> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

**MySQL connection errors:**
- Ensure Docker is running: `docker ps`
- Restart MySQL container: `docker-compose restart`
- Check connection settings in `application.yml`

**Flyway migration errors:**
- Drop and recreate database: `docker-compose down -v && docker-compose up -d`

### Dashboard Issues

**Port 4200 already in use:**
```bash
# Kill process on port 4200
npx kill-port 4200
```

**Backend connection errors:**
- Verify backend is running at `http://localhost:8080`
- Check `src/environments/environment.ts` for correct API URL
- Check browser console for CORS errors

**Login not working:**
- Ensure backend is running and migrations have run
- Verify test users exist in database (check `V10__insert_test_dashboard_users.sql`)
- Clear browser localStorage: `localStorage.clear()`

---

## Future Enhancements

### Completed ✅
- JWT authentication for dashboard
- Angular admin dashboard with analytics
- Real-time metrics and reporting
- User leaderboards
- Time-series analytics

### Planned 🚀
- Campaign CRUD in dashboard UI
- Referral management interface
- Email/SMS notifications
- QR code referral links
- Webhook callbacks for real-time updates
- Advanced fraud detection
- Multi-region deployment
- Event-driven architecture with Kafka/RabbitMQ
- Export functionality (CSV, PDF reports)
- Advanced filtering and search
- Role-based access control (RBAC)
- White-label dashboard customization

---

## Development Tips

### Backend Development

1. **Hot Reload**: Spring Boot DevTools is included - code changes will auto-reload
2. **SQL Logging**: Enable in `application.yml` with `spring.jpa.show-sql: true`
3. **Database Inspection**: Use MySQL Workbench or connect via:
   ```bash
   docker exec -it referralpro-mysql-1 mysql -uroot -prootpassword referral_platform
   ```
4. **API Testing**: Use the included Postman collection: `ReferralPro_API.postman_collection.json`
5. **Debug Mode**: Run with `.\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug"`

### Dashboard Development

1. **Auto Reload**: Angular dev server auto-reloads on file changes
2. **API Proxy**: To avoid CORS issues, configure proxy in `angular.json` or use browser extension
3. **State Management**: Auth state is stored in localStorage - check DevTools → Application → Local Storage
4. **Network Debugging**: Open browser DevTools → Network to inspect API calls
5. **Component Testing**: Each component should have a `.spec.ts` file for unit tests

### Full Stack Development Workflow

1. Start MySQL: `docker-compose up -d`
2. Start backend in one terminal: `.\mvnw.cmd spring-boot:run`
3. Start dashboard in another terminal: `cd referralPro-dashboard && npm start`
4. Open dashboard at `http://localhost:4200`
5. Use Swagger UI at `http://localhost:8080/swagger-ui.html` for API testing

### Code Organization

- **Backend**: Organize by feature (e.g., `campaign/`, `referral/`), not by layer
- **Frontend**: Use `core/` for services/guards, `features/` for routes, `shared/` for reusables
- **DTOs**: Use Java records for immutable request/response objects
- **Models**: Define TypeScript interfaces in `shared/models/` for type safety

---

## API Documentation

### Swagger/OpenAPI

Interactive API documentation is available via Swagger UI:
- **URL**: `http://localhost:8080/swagger-ui.html`
- **Features**: Test all endpoints, view request/response schemas, authentication setup

### Postman Collection

A Postman collection is included in the repository:
- **File**: `ReferralPro_API.postman_collection.json`
- **Import**: Open Postman → Import → Select file
- **Usage**: Update environment variables for `baseUrl` and `apiKey`

### Key API Endpoints Summary

| Category | Method | Endpoint | Auth | Description |
|----------|--------|----------|------|-------------|
| **Company** | POST | `/api/companies/register` | None | Register new company |
| **Campaign** | POST | `/api/companies/{id}/campaigns` | API Key | Create campaign |
| **Referral** | POST | `/api/referrals/generate` | API Key | Generate referral link |
| **Click** | GET | `/r/{code}` | None | Track click & redirect |
| **Conversion** | POST | `/api/conversions` | API Key | Record conversion |
| **Reward** | GET | `/api/rewards/users/{id}` | API Key | Get user rewards |
| **Auth** | POST | `/api/auth/login` | None | Dashboard login |
| **Dashboard** | GET | `/api/dashboard/overview` | JWT | Get metrics |

---

## License

Proprietary

## Support

For issues or questions, contact the development team.
