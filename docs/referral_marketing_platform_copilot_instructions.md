# Referral Marketing Platform - GitHub Copilot Agent Build Instructions

## 1. Product Goal

Build a multi-tenant referral marketing platform using Spring Boot.

The platform allows companies to register, create referral marketing campaigns, generate referral links for their customers, track referred users, detect successful conversions, and issue rewards or discounts to both the referrer and the referred friend.

Example flow:

1. A company registers on the platform.
2. The company creates a marketing/referral campaign.
3. A customer completes a service on the company's platform.
4. The company calls our API to generate a referral link for that customer.
5. The customer shares the referral link with a friend.
6. The friend clicks the link and registers on the company's platform.
7. The friend completes a required service.
8. The company calls our conversion API.
9. Our platform validates the referral and issues rewards to both users.

---

## 2. Tech Stack

Use the following stack:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security
- JWT authentication for dashboard users
- API key authentication for company integrations
- MySQL database
- Flyway for database migration
- Maven
- Lombok
- Docker
- OpenAPI/Swagger
- JUnit 5 and Mockito

---

## 3. Core Architecture

Use modular package-by-feature architecture.

Suggested root package:

```text
com.actpro.referral
```

Suggested structure:

```text
src/main/java/com/actpro/referral

├── company
│   ├── Company.java
│   ├── CompanyRepository.java
│   ├── CompanyService.java
│   ├── CompanyController.java
│   └── dto
│
├── campaign
│   ├── Campaign.java
│   ├── CampaignRepository.java
│   ├── CampaignService.java
│   ├── CampaignController.java
│   └── dto
│
├── user
│   ├── PlatformUser.java
│   ├── PlatformUserRepository.java
│   ├── PlatformUserService.java
│   └── dto
│
├── referral
│   ├── Referral.java
│   ├── ReferralRepository.java
│   ├── ReferralService.java
│   ├── ReferralController.java
│   ├── ReferralCodeGenerator.java
│   └── dto
│
├── click
│   ├── ReferralClick.java
│   ├── ReferralClickRepository.java
│   └── ReferralClickService.java
│
├── conversion
│   ├── Conversion.java
│   ├── ConversionRepository.java
│   ├── ConversionService.java
│   ├── ConversionController.java
│   └── dto
│
├── reward
│   ├── Reward.java
│   ├── RewardRepository.java
│   ├── RewardService.java
│   ├── CouponCodeGenerator.java
│   └── dto
│
├── auth
│   ├── DashboardUser.java
│   ├── DashboardUserRepository.java
│   ├── AuthController.java
│   ├── JwtService.java
│   └── dto
│
├── security
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── ApiKeyAuthenticationFilter.java
│   └── CompanyContext.java
│
└── common
    ├── BaseEntity.java
    ├── ApiResponse.java
    ├── ErrorResponse.java
    ├── GlobalExceptionHandler.java
    └── exception
```

---

## 4. Main Domain Model

### 4.1 Company

Represents a business using the referral platform.

Fields:

```text
id: Long
name: String
email: String
status: CompanyStatus
apiKey: String
createdAt: LocalDateTime
updatedAt: LocalDateTime
```

CompanyStatus enum:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Rules:

- Company email must be unique.
- API key must be generated during company registration.
- API key is used by the company's backend to call integration APIs.

---

### 4.2 Campaign

Represents a referral campaign created by a company.

Fields:

```text
id: Long
company: Company
name: String
description: String
landingPageUrl: String
startDate: LocalDateTime
endDate: LocalDateTime
rewardType: RewardType
referrerRewardValue: BigDecimal
refereeRewardValue: BigDecimal
conversionEventName: String
status: CampaignStatus
createdAt: LocalDateTime
updatedAt: LocalDateTime
```

CampaignStatus enum:

```text
DRAFT
ACTIVE
PAUSED
EXPIRED
```

RewardType enum:

```text
DISCOUNT_AMOUNT
DISCOUNT_PERCENTAGE
CREDIT
POINTS
```

Rules:

- Campaign must belong to a company.
- Only active campaigns can generate valid referral conversions.
- Campaign end date must be after start date.
- `landingPageUrl` is where referred users will be redirected.

---

### 4.3 PlatformUser

Represents a customer/end user of a company.

Fields:

```text
id: Long
company: Company
externalUserId: String
email: String
name: String
createdAt: LocalDateTime
updatedAt: LocalDateTime
```

Rules:

- `externalUserId` comes from the company's own system.
- Combination of `companyId + externalUserId` must be unique.
- The same email may exist across different companies.

---

### 4.4 Referral

Represents a referral link generated for a referrer.

Fields:

```text
id: Long
company: Company
campaign: Campaign
referrerUser: PlatformUser
referralCode: String
referralLink: String
status: ReferralStatus
createdAt: LocalDateTime
updatedAt: LocalDateTime
```

ReferralStatus enum:

```text
ACTIVE
CONVERTED
EXPIRED
CANCELLED
```

Rules:

- Referral code must be globally unique.
- Referral belongs to one campaign and one referrer.
- Referral code should be short and shareable.
- Example referral link: `https://yourdomain.com/r/ABC12345`

---

### 4.5 ReferralClick

Tracks clicks on referral links.

Fields:

```text
id: Long
referral: Referral
ipAddress: String
userAgent: String
clickedAt: LocalDateTime
```

Rules:

- Every click on `/r/{referralCode}` should be recorded.
- After recording the click, redirect the user to the campaign landing page with the referral code appended.

Example redirect:

```text
https://company.com/signup?ref=ABC12345
```

---

### 4.6 Conversion

Represents a successful referred-user action.

Fields:

```text
id: Long
company: Company
campaign: Campaign
referral: Referral
referrerUser: PlatformUser
refereeUser: PlatformUser
eventName: String
status: ConversionStatus
completedAt: LocalDateTime
createdAt: LocalDateTime
```

ConversionStatus enum:

```text
PENDING
COMPLETED
REJECTED
REWARDED
```

Rules:

- A conversion happens when the referred friend completes the campaign's required service.
- Do not allow self-referral.
- Do not allow duplicate rewards for the same referee and referral.
- Event name must match the campaign conversion event name.

---

### 4.7 Reward

Represents a discount, credit, coupon, or points issued to a user.

Fields:

```text
id: Long
company: Company
campaign: Campaign
conversion: Conversion
user: PlatformUser
rewardType: RewardType
rewardValue: BigDecimal
couponCode: String
status: RewardStatus
createdAt: LocalDateTime
redeemedAt: LocalDateTime
```

RewardStatus enum:

```text
ISSUED
REDEEMED
EXPIRED
CANCELLED
```

Rules:

- A successful conversion creates two rewards:
  - one for the referrer
  - one for the referee
- Coupon code must be unique.
- Reward should not be issued twice for the same conversion/user pair.

---

## 5. Required APIs

### 5.1 Company Registration API

```http
POST /api/companies/register
```

Request:

```json
{
  "name": "ABC Cleaning",
  "email": "admin@abc-cleaning.com"
}
```

Response:

```json
{
  "companyId": 1,
  "name": "ABC Cleaning",
  "apiKey": "cmp_live_generated_api_key"
}
```

Behavior:

- Create company.
- Generate API key.
- Return API key once.

---

### 5.2 Create Campaign API

```http
POST /api/companies/{companyId}/campaigns
Authorization: Bearer {jwt}
```

Request:

```json
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

Response:

```json
{
  "campaignId": 100,
  "status": "ACTIVE"
}
```

---

### 5.3 Generate Referral Link API

This API is called by the company's backend when a customer completes a service and becomes eligible to refer friends.

```http
POST /api/referrals/generate
Authorization: ApiKey cmp_live_generated_api_key
```

Request:

```json
{
  "campaignId": 100,
  "externalUserId": "cust_5001",
  "email": "john@test.com",
  "name": "John Smith"
}
```

Response:

```json
{
  "referralCode": "ABC12345",
  "referralLink": "https://yourdomain.com/r/ABC12345"
}
```

Behavior:

- Resolve company from API key.
- Find campaign.
- Create or find platform user by `companyId + externalUserId`.
- Generate unique referral code.
- Save referral.
- Return shareable referral link.

---

### 5.4 Referral Redirect API

```http
GET /r/{referralCode}
```

Behavior:

- Find referral by referral code.
- Record click with IP address and user agent.
- Redirect to campaign landing page with `ref` query parameter.

Example:

```text
/r/ABC12345
```

Redirects to:

```text
https://abc-cleaning.com/signup?ref=ABC12345
```

---

### 5.5 Attach Referred Friend API

This API is called by the company's backend after a referred friend registers.

```http
POST /api/referrals/attach-referee
Authorization: ApiKey cmp_live_generated_api_key
```

Request:

```json
{
  "referralCode": "ABC12345",
  "externalUserId": "cust_9009",
  "email": "friend@test.com",
  "name": "Friend User"
}
```

Behavior:

- Resolve company from API key.
- Find referral by code.
- Create or find referee user.
- Validate referee is not same as referrer.
- This step may be optional if conversion API provides the same referee information.

---

### 5.6 Conversion API

This API is called by the company's backend when the referred friend completes the required service.

```http
POST /api/conversions
Authorization: ApiKey cmp_live_generated_api_key
```

Request:

```json
{
  "referralCode": "ABC12345",
  "externalUserId": "cust_9009",
  "email": "friend@test.com",
  "name": "Friend User",
  "eventName": "SERVICE_COMPLETED"
}
```

Response:

```json
{
  "conversionId": 300,
  "status": "REWARDED",
  "referrerReward": {
    "couponCode": "REF-A1B2C3D4",
    "rewardValue": 10.00
  },
  "refereeReward": {
    "couponCode": "REF-E5F6G7H8",
    "rewardValue": 10.00
  }
}
```

Behavior:

- Resolve company from API key.
- Find referral.
- Find campaign.
- Validate campaign is active.
- Validate event name matches campaign conversion event name.
- Create or find referee user.
- Reject self-referral.
- Reject duplicate conversion.
- Create conversion.
- Issue rewards to referrer and referee.
- Mark conversion as rewarded.

---

### 5.7 Get Rewards by User API

```http
GET /api/rewards/users/{externalUserId}
Authorization: ApiKey cmp_live_generated_api_key
```

Response:

```json
{
  "externalUserId": "cust_5001",
  "rewards": [
    {
      "couponCode": "REF-A1B2C3D4",
      "rewardType": "DISCOUNT_AMOUNT",
      "rewardValue": 10.00,
      "status": "ISSUED"
    }
  ]
}
```

---

## 6. Core Service Logic

### 6.1 ReferralService.generateReferral

Implement this behavior:

```java
@Transactional
public GenerateReferralResponse generateReferral(GenerateReferralRequest request) {
    Company company = companyContext.getCurrentCompany();

    Campaign campaign = campaignRepository
        .findByIdAndCompanyId(request.campaignId(), company.getId())
        .orElseThrow(() -> new NotFoundException("Campaign not found"));

    if (!campaign.isActive()) {
        throw new BadRequestException("Campaign is not active");
    }

    PlatformUser referrer = platformUserService.findOrCreate(
        company,
        request.externalUserId(),
        request.email(),
        request.name()
    );

    String code = referralCodeGenerator.generateUniqueCode();
    String link = baseUrl + "/r/" + code;

    Referral referral = new Referral();
    referral.setCompany(company);
    referral.setCampaign(campaign);
    referral.setReferrerUser(referrer);
    referral.setReferralCode(code);
    referral.setReferralLink(link);
    referral.setStatus(ReferralStatus.ACTIVE);

    referralRepository.save(referral);

    return new GenerateReferralResponse(code, link);
}
```

---

### 6.2 ConversionService.completeConversion

Implement this behavior:

```java
@Transactional
public ConversionResponse completeConversion(ConversionRequest request) {
    Company company = companyContext.getCurrentCompany();

    Referral referral = referralRepository
        .findByReferralCodeAndCompanyId(request.referralCode(), company.getId())
        .orElseThrow(() -> new NotFoundException("Referral not found"));

    Campaign campaign = referral.getCampaign();

    if (!campaign.isActive()) {
        throw new BadRequestException("Campaign is not active");
    }

    if (!campaign.getConversionEventName().equals(request.eventName())) {
        throw new BadRequestException("Invalid conversion event");
    }

    PlatformUser referee = platformUserService.findOrCreate(
        company,
        request.externalUserId(),
        request.email(),
        request.name()
    );

    if (referral.getReferrerUser().getId().equals(referee.getId())) {
        throw new BadRequestException("Self referral is not allowed");
    }

    boolean duplicate = conversionRepository.existsByReferralIdAndRefereeUserId(
        referral.getId(),
        referee.getId()
    );

    if (duplicate) {
        throw new BadRequestException("Referral already converted for this user");
    }

    Conversion conversion = new Conversion();
    conversion.setCompany(company);
    conversion.setCampaign(campaign);
    conversion.setReferral(referral);
    conversion.setReferrerUser(referral.getReferrerUser());
    conversion.setRefereeUser(referee);
    conversion.setEventName(request.eventName());
    conversion.setStatus(ConversionStatus.COMPLETED);
    conversion.setCompletedAt(LocalDateTime.now());

    conversionRepository.save(conversion);

    RewardResult rewardResult = rewardService.issueRewards(conversion);

    conversion.setStatus(ConversionStatus.REWARDED);
    conversionRepository.save(conversion);

    return ConversionResponse.from(conversion, rewardResult);
}
```

---

### 6.3 RewardService.issueRewards

Implement this behavior:

```java
@Transactional
public RewardResult issueRewards(Conversion conversion) {
    Campaign campaign = conversion.getCampaign();

    Reward referrerReward = createReward(
        conversion,
        conversion.getReferrerUser(),
        campaign.getReferrerRewardValue()
    );

    Reward refereeReward = createReward(
        conversion,
        conversion.getRefereeUser(),
        campaign.getRefereeRewardValue()
    );

    rewardRepository.save(referrerReward);
    rewardRepository.save(refereeReward);

    return new RewardResult(referrerReward, refereeReward);
}

private Reward createReward(Conversion conversion, PlatformUser user, BigDecimal value) {
    Reward reward = new Reward();
    reward.setCompany(conversion.getCompany());
    reward.setCampaign(conversion.getCampaign());
    reward.setConversion(conversion);
    reward.setUser(user);
    reward.setRewardType(conversion.getCampaign().getRewardType());
    reward.setRewardValue(value);
    reward.setCouponCode(couponCodeGenerator.generate());
    reward.setStatus(RewardStatus.ISSUED);
    return reward;
}
```

---

## 7. Database Migration Plan

Create Flyway migrations.

Start with:

```text
V1__create_companies.sql
V2__create_campaigns.sql
V3__create_platform_users.sql
V4__create_referrals.sql
V5__create_referral_clicks.sql
V6__create_conversions.sql
V7__create_rewards.sql
V8__create_dashboard_users.sql
```

Important unique constraints:

```sql
unique companies.email
unique companies.api_key
unique platform_users(company_id, external_user_id)
unique referrals.referral_code
unique rewards.coupon_code
unique conversions(referral_id, referee_user_id)
unique rewards(conversion_id, user_id)
```

---

## 8. Security Requirements

### 8.1 Dashboard Security

Use JWT authentication for company admins using the dashboard.

Roles:

```text
SUPER_ADMIN
COMPANY_ADMIN
COMPANY_MARKETER
```

Protected endpoints:

```text
/api/companies/{companyId}/campaigns
/api/dashboard/**
```

---

### 8.2 API Key Security

Company backend integrations should use API key authentication.

Header format:

```http
Authorization: ApiKey cmp_live_xxxxx
```

The `ApiKeyAuthenticationFilter` should:

1. Read the Authorization header.
2. Check if it starts with `ApiKey `.
3. Extract API key.
4. Find company by API key.
5. Store company in `CompanyContext`.
6. Reject request if API key is invalid.

Protected integration endpoints:

```text
/api/referrals/generate
/api/referrals/attach-referee
/api/conversions
/api/rewards/users/{externalUserId}
```

Public endpoint:

```text
GET /r/{referralCode}
```

---

## 9. Validation Rules

Implement these validations:

```text
Company name is required.
Company email is required and unique.
Campaign name is required.
Campaign landing page URL is required.
Campaign end date must be after start date.
Campaign must be ACTIVE for referral generation and conversion.
Referral code must exist.
Referral must belong to the company resolved from API key.
Referrer and referee cannot be the same user.
Conversion event name must match campaign conversion event name.
Duplicate conversion is not allowed.
Reward cannot be issued twice for same conversion and user.
```

---

## 10. MVP Build Order for Copilot Agent

Please build the project in this exact order.

### Phase 1: Project setup

Create a Spring Boot 3 project with:

- Java 21
- Maven
- Spring Web
- Spring Data JPA
- Spring Security
- MySQL Driver
- Flyway
- Lombok
- Validation
- Springdoc OpenAPI

Add Dockerfile and docker-compose.yml with MySQL.

---

### Phase 2: Common foundation

Create:

- BaseEntity
- ApiResponse
- ErrorResponse
- GlobalExceptionHandler
- Custom exceptions:
  - NotFoundException
  - BadRequestException
  - UnauthorizedException

---

### Phase 3: Company module

Create:

- Company entity
- CompanyStatus enum
- CompanyRepository
- CompanyService
- CompanyController
- RegisterCompanyRequest
- RegisterCompanyResponse
- API key generator

Endpoint:

```http
POST /api/companies/register
```

---

### Phase 4: Campaign module

Create:

- Campaign entity
- CampaignStatus enum
- RewardType enum
- CampaignRepository
- CampaignService
- CampaignController
- CreateCampaignRequest
- CampaignResponse

Endpoint:

```http
POST /api/companies/{companyId}/campaigns
GET /api/companies/{companyId}/campaigns
```

---

### Phase 5: Platform user module

Create:

- PlatformUser entity
- PlatformUserRepository
- PlatformUserService
- findOrCreate method using company + externalUserId

---

### Phase 6: API key security

Create:

- ApiKeyAuthenticationFilter
- CompanyContext
- SecurityConfig updates

Protect integration APIs with API key auth.

---

### Phase 7: Referral module

Create:

- Referral entity
- ReferralStatus enum
- ReferralRepository
- ReferralCodeGenerator
- ReferralService
- ReferralController
- GenerateReferralRequest
- GenerateReferralResponse

Endpoint:

```http
POST /api/referrals/generate
```

---

### Phase 8: Referral click and redirect

Create:

- ReferralClick entity
- ReferralClickRepository
- ReferralClickService

Endpoint:

```http
GET /r/{referralCode}
```

Behavior:

- Save click.
- Redirect to campaign landing page with referral code.

---

### Phase 9: Conversion module

Create:

- Conversion entity
- ConversionStatus enum
- ConversionRepository
- ConversionService
- ConversionController
- ConversionRequest
- ConversionResponse

Endpoint:

```http
POST /api/conversions
```

---

### Phase 10: Reward module

Create:

- Reward entity
- RewardStatus enum
- RewardRepository
- CouponCodeGenerator
- RewardService
- RewardResponse

Endpoints:

```http
GET /api/rewards/users/{externalUserId}
```

---

### Phase 11: Tests

Add unit tests for:

- Company registration
- Campaign creation
- Referral generation
- Self-referral rejection
- Duplicate conversion rejection
- Successful conversion reward generation

Add integration tests for:

- Generate referral API
- Conversion API
- Get rewards API

---

## 11. Example Local Development Setup

### docker-compose.yml

Create MySQL service:

```yaml
version: "3.9"

services:
  mysql:
    image: mysql:8.4
    container_name: referral-mysql
    environment:
      MYSQL_DATABASE: referral_platform
      MYSQL_USER: referral_user
      MYSQL_PASSWORD: referral_pass
      MYSQL_ROOT_PASSWORD: root_pass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/referral_platform
    username: referral_user
    password: referral_pass
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
    show-sql: true
  flyway:
    enabled: true

app:
  base-url: http://localhost:8080
  jwt:
    secret: change-this-secret
    expiration-minutes: 120
```

---

## 12. Sample API Testing Flow

### Step 1: Register company

```http
POST http://localhost:8080/api/companies/register
```

```json
{
  "name": "ABC Cleaning",
  "email": "admin@abc-cleaning.com"
}
```

Save the returned API key.

---

### Step 2: Create campaign

```http
POST http://localhost:8080/api/companies/1/campaigns
```

```json
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

---

### Step 3: Generate referral link

```http
POST http://localhost:8080/api/referrals/generate
Authorization: ApiKey cmp_live_generated_api_key
```

```json
{
  "campaignId": 1,
  "externalUserId": "cust_5001",
  "email": "john@test.com",
  "name": "John Smith"
}
```

---

### Step 4: Friend clicks referral link

```http
GET http://localhost:8080/r/ABC12345
```

---

### Step 5: Friend completes service

```http
POST http://localhost:8080/api/conversions
Authorization: ApiKey cmp_live_generated_api_key
```

```json
{
  "referralCode": "ABC12345",
  "externalUserId": "cust_9009",
  "email": "friend@test.com",
  "name": "Friend User",
  "eventName": "SERVICE_COMPLETED"
}
```

---

### Step 6: Get rewards

```http
GET http://localhost:8080/api/rewards/users/cust_5001
Authorization: ApiKey cmp_live_generated_api_key
```

---

## 13. Future Enhancements After MVP

Do not build these in the first version, but keep the design open for them:

- Email invitations
- SMS referral links
- QR-code referral links
- Webhook callbacks to companies
- Analytics dashboard
- Campaign performance reports
- Fraud detection
- Reward expiration rules
- Stripe billing for companies
- Multi-region deployment
- Kafka/RabbitMQ event-driven reward processing
- React dashboard
- Admin approval workflow

---

## 14. Final Instruction to Copilot Agent

Build this project step by step. Do not generate everything in one giant file. Follow the package structure. Create clean entities, DTOs, repositories, services, controllers, security filters, Flyway migrations, and tests.

Prioritize a working MVP over advanced features. The first working version must support:

1. Company registration
2. Campaign creation
3. Referral link generation
4. Referral link redirect and click tracking
5. Conversion submission
6. Reward generation for both referrer and referee
7. Reward lookup by user

