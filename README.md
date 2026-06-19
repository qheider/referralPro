# Referral Marketing Platform

A multi-tenant referral marketing platform built with Spring Boot 3.x and Java 21.

## Features

- **Multi-tenant architecture**: Support multiple companies with isolated data
- **API key authentication**: Secure integration for company backends
- **Campaign management**: Create and manage referral campaigns with custom rewards
- **Referral link generation**: Generate unique shareable referral links
- **Click tracking**: Track referral link clicks with IP and user agent
- **Conversion tracking**: Validate and track successful referrals
- **Automatic reward issuance**: Issue rewards to both referrer and referee
- **Reward lookup**: Query rewards by user

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring Data JPA
- Spring Security
- MySQL 8.4
- Flyway (database migrations)
- Lombok
- Maven
- Docker & Docker Compose
- OpenAPI/Swagger UI

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose

## Getting Started

### 1. Start MySQL Database

```bash
docker-compose up -d
```

This will start a MySQL 8.4 container on port 3306.

### 2. Build the Application

```bash
./mvnw clean install
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

### 4. Access Swagger UI

Open your browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```

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

## Database Schema

The application uses Flyway for database migrations. The schema includes:

- **companies**: Company accounts with API keys
- **campaigns**: Referral campaigns with reward configuration
- **platform_users**: End users (customers) of companies
- **referrals**: Generated referral links
- **referral_clicks**: Click tracking data
- **conversions**: Successful referral conversions
- **rewards**: Issued rewards (coupons/credits)
- **dashboard_users**: Admin users (optional, for future JWT auth)

## Configuration

Main configuration is in `src/main/resources/application.yml`:

- **Database**: MySQL connection settings
- **JPA**: Hibernate and SQL logging
- **Flyway**: Database migration settings
- **App**: Base URL for referral links, JWT secret

## Project Structure

```
src/main/java/com/actpro/referral/
├── company/          # Company registration and management
├── campaign/         # Campaign CRUD operations
├── user/             # Platform user management
├── referral/         # Referral link generation and redirect
├── click/            # Click tracking
├── conversion/       # Conversion processing
├── reward/           # Reward issuance and lookup
├── security/         # API key authentication
├── common/           # Shared utilities and exceptions
└── config/           # Application configuration
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

- **API Key Authentication**: All integration endpoints require a valid API key in the `Authorization: ApiKey {key}` header
- **Company Isolation**: API key determines which company's data is accessible
- **Public Endpoints**: `/r/{referralCode}` (redirect) and `/api/companies/register` are publicly accessible

## Testing

Run unit and integration tests:

```bash
./mvnw test
```

## Future Enhancements

- JWT authentication for dashboard
- React admin dashboard
- Email/SMS notifications
- QR code referral links
- Webhook callbacks
- Analytics and reporting
- Fraud detection
- Multi-region deployment
- Event-driven architecture with Kafka/RabbitMQ

## License

Proprietary

## Support

For issues or questions, contact the development team.
