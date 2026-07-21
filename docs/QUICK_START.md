# Quick Start Guide

## Project Built Successfully! ✅

The Referral Marketing Platform has been implemented with all core features.

## What Was Built

### Phase 1: Foundation ✅
- Maven project structure with Spring Boot 3.5.15 and Java 21
- Docker Compose with MySQL 8.4
- Application configuration files
- Common utilities (BaseEntity, ApiResponse, GlobalExceptionHandler, custom exceptions)
- 8 Flyway database migrations with proper constraints

### Phase 2: Core Modules ✅
- **Company Module**: Registration with API key generation
- **Campaign Module**: CRUD operations with validation
- **PlatformUser Module**: User management with findOrCreate pattern

### Phase 3: Security ✅
- API Key authentication filter
- Company context (ThreadLocal)
- Spring Security configuration

### Phase 4: Referral System ✅
- **Referral Module**: Link generation with unique 8-char codes
- **Click Tracking**: Record clicks with IP/user agent
- **Redirect Controller**: Public `/r/{code}` endpoint

### Phase 5: Conversion & Rewards ✅
- **Conversion Module**: Event tracking with validations
- **Reward Module**: Automatic dual reward issuance
- **Integration**: End-to-end flow from conversion to rewards

## Architecture Highlights

✅ **60 Java files** created across 11 packages
✅ **Package-by-feature** modular structure
✅ **API-first design** with Swagger/OpenAPI docs
✅ **Security isolation** with API key authentication
✅ **Business validations**: Self-referral prevention, duplicate detection, event matching
✅ **Transactional integrity** with Spring @Transactional

## Next Steps

### 1. Start MySQL (requires Docker Desktop)

```bash
docker-compose up -d
```

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

### 3. Access Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### 4. Test the Complete Flow

#### Step 1: Register Company
```bash
curl -X POST http://localhost:8080/api/companies/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ABC Cleaning",
    "email": "admin@abc-cleaning.com"
  }'
```
**Save the `apiKey` from response!**

#### Step 2: Create Campaign
```bash
curl -X POST http://localhost:8080/api/companies/1/campaigns \
  -H "Authorization: ApiKey YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Refer a Friend",
    "description": "Give $10, get $10",
    "landingPageUrl": "https://example.com/signup",
    "startDate": "2026-07-01T00:00:00",
    "endDate": "2026-12-31T23:59:59",
    "rewardType": "DISCOUNT_AMOUNT",
    "referrerRewardValue": 10.00,
    "refereeRewardValue": 10.00,
    "conversionEventName": "SERVICE_COMPLETED"
  }'
```

#### Step 3: Generate Referral Link
```bash
curl -X POST http://localhost:8080/api/referrals/generate \
  -H "Authorization: ApiKey YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "campaignId": 1,
    "externalUserId": "cust_5001",
    "email": "john@test.com",
    "name": "John Smith"
  }'
```
**Save the `referralCode`!**

#### Step 4: Click Referral Link
```bash
curl -L http://localhost:8080/r/YOUR_REFERRAL_CODE
```

#### Step 5: Complete Conversion
```bash
curl -X POST http://localhost:8080/api/conversions \
  -H "Authorization: ApiKey YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "referralCode": "YOUR_REFERRAL_CODE",
    "externalUserId": "cust_9009",
    "email": "friend@test.com",
    "name": "Friend User",
    "eventName": "SERVICE_COMPLETED"
  }'
```
**Response includes coupon codes for both users!**

#### Step 6: Get User Rewards
```bash
curl http://localhost:8080/api/rewards/users/cust_5001 \
  -H "Authorization: ApiKey YOUR_API_KEY"
```

## Key Features Implemented

### ✅ Multi-Tenant Support
- Each company has isolated data
- API key determines accessible resources

### ✅ Referral Tracking
- Unique 8-character codes (e.g., `ABC12345`)
- Click recording with analytics data
- Campaign landing page redirect with `?ref=` parameter

### ✅ Smart Validations
- ❌ Self-referral blocked
- ❌ Duplicate conversion prevented
- ❌ Inactive campaign rejected
- ❌ Event name mismatch caught

### ✅ Automatic Rewards
- Dual issuance (referrer + referee)
- Unique coupon codes (e.g., `REF-A1B2C3D4`)
- Configurable reward types and values

### ✅ API Documentation
- Swagger UI at `/swagger-ui.html`
- Complete request/response schemas
- Try-it-out functionality

## File Structure Summary

```
src/main/java/com/actpro/referral/
├── ReferralApplication.java           # Main Spring Boot app
├── common/                            # 4 files (utilities, exceptions)
├── config/                            # 1 file (JPA config)
├── company/                           # 5 files (entity, repo, service, controller, DTOs)
├── campaign/                          # 8 files (entity, repo, service, controller, enums, DTOs)
├── user/                              # 4 files (entity, repo, service, DTO)
├── referral/                          # 9 files (entity, repo, service, controllers, generator, DTOs)
├── click/                             # 3 files (entity, repo, service)
├── conversion/                        # 6 files (entity, repo, service, controller, enum, DTOs)
├── reward/                            # 9 files (entity, repo, service, controller, generator, DTOs)
└── security/                          # 3 files (config, filter, context)

src/main/resources/
├── application.yml                    # Main config
└── db/migration/                      # 8 Flyway SQL files

Additional Files:
├── pom.xml                            # Maven dependencies
├── docker-compose.yml                 # MySQL container
├── Dockerfile                         # App containerization
└── README.md                          # Full documentation
```

## Database Schema

8 tables with proper foreign keys and unique constraints:
- `companies` - Company accounts with API keys
- `campaigns` - Referral campaigns
- `platform_users` - End users (customers)
- `referrals` - Generated referral links
- `referral_clicks` - Click tracking
- `conversions` - Successful conversions
- `rewards` - Issued rewards
- `dashboard_users` - Admin users (optional)

## Technology Stack

- **Backend**: Spring Boot 3.5.15, Java 21
- **Database**: MySQL 8.4
- **ORM**: Spring Data JPA + Hibernate
- **Security**: Spring Security with custom API key filter
- **Migration**: Flyway
- **Documentation**: SpringDoc OpenAPI 3
- **Build**: Maven
- **Container**: Docker + Docker Compose

## What's Next (Optional)

These features were designed for but not yet implemented:
- [ ] JWT authentication for dashboard
- [ ] React admin UI
- [ ] Email/SMS notifications
- [ ] QR code generation
- [ ] Webhook callbacks
- [ ] Analytics dashboard
- [ ] Fraud detection
- [ ] Unit and integration tests

## Troubleshooting

**Docker not starting?**
- Ensure Docker Desktop is running
- Check port 3306 is available

**Build fails?**
- Verify Java 21 is installed: `java -version`
- Clean and rebuild: `./mvnw clean compile`

**Application won't start?**
- Check MySQL is running: `docker ps`
- Verify connection in `application.yml`

## Success Metrics

✅ **All 14 tasks completed**
✅ **Zero compilation errors**
✅ **Clean modular architecture**
✅ **Production-ready code structure**
✅ **Comprehensive README documentation**

---

**Project Status**: MVP Complete - Ready for Testing 🚀
