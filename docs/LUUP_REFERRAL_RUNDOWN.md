# Luup Referral Program — Rundown

## Overview
Luup integrates with the Referral Marketing Platform via REST API using an API key.
The platform handles link generation, click tracking, conversion validation, and reward issuance automatically.

---

## Step 1 — Register Luup (One-Time)

**No auth required.**

```bash
POST http://localhost:8080/api/companies/register
Content-Type: application/json

{
  "name": "Luup",
  "email": "admin@luup.com"
}
```

→ Save the returned `apiKey`. All subsequent calls require:
  ```
  Header: Authorization: ApiKey <your-api-key>
  ```

---

## Step 2 — Create a Campaign (One-Time per Program)

```bash
POST http://localhost:8080/api/companies/{companyId}/campaigns
Authorization: ApiKey <your-api-key>
Content-Type: application/json

{
  "name": "Luup Refer a Rider",
  "description": "Give 20% off, Get 20% off your next ride",
  "landingPageUrl": "https://luup.com/signup",
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-12-31T23:59:59",
  "rewardType": "DISCOUNT_PERCENTAGE",
  "referrerRewardValue": 20.00,
  "refereeRewardValue": 20.00,
  "conversionEventName": "FIRST_RIDE_COMPLETED"
}
```

**Reward type options:**
- `DISCOUNT_PERCENTAGE` — % off next transaction
- `DISCOUNT_AMOUNT` — Fixed dollar/unit amount off
- `CREDIT` — Account credit
- `POINTS` — Loyalty points

→ Save the returned `id` (campaignId).

---

## Step 3 — Generate a Referral Link (Per User, On Demand)

Called from Luup's backend when a user taps "Share & Earn".

```bash
POST http://localhost:8080/api/referrals/generate
Authorization: ApiKey <your-api-key>
Content-Type: application/json

{
  "campaignId": 1,
  "externalUserId": "luup_user_1042",
  "email": "sara@example.com",
  "name": "Sara Ahmed"
}
```

**Response:**
```json
{
  "referralCode": "LUP3X9KM",
  "referralLink": "http://localhost:8080/r/LUP3X9KM"
}
```

→ Share the link via app/SMS/email. Each user gets a unique 8-character code.

---

## Step 4 — Friend Clicks the Link (Automatic)

```bash
GET http://localhost:8080/r/{referralCode}
```

No integration needed. The platform:
- Records the click (IP, user agent, timestamp)
- Redirects friend to: `https://luup.com/signup?ref={referralCode}`

---

## Step 5 — Record a Conversion (Triggered by Luup's Backend)

Called when the friend completes their first ride.

```bash
POST http://localhost:8080/api/conversions
Authorization: ApiKey <your-api-key>
Content-Type: application/json

{
  "referralCode": "LUP3X9KM",
  "externalUserId": "luup_user_8831",
  "email": "karim@example.com",
  "name": "Karim Hassan",
  "eventName": "FIRST_RIDE_COMPLETED"
}
```

**Response automatically issues two coupon codes:**
```json
{
  "referrerReward": {
    "couponCode": "REF-A1B2C3D4",
    "rewardType": "DISCOUNT_PERCENTAGE",
    "rewardValue": 20.0,
    "userId": "luup_user_1042"
  },
  "refereeReward": {
    "couponCode": "REF-E5F6G7H8",
    "rewardType": "DISCOUNT_PERCENTAGE",
    "rewardValue": 20.0,
    "userId": "luup_user_8831"
  }
}
```

**Business rules enforced automatically:**
- Self-referral blocked
- Duplicate conversion blocked
- `eventName` must match campaign's `conversionEventName` exactly
- Campaign must be within active date range

---

## Step 6 — Fetch User Rewards (For In-App Display)

```bash
GET http://localhost:8080/api/rewards/users/{externalUserId}
Authorization: ApiKey <your-api-key>
```

→ Returns all rewards earned by the user (coupons, credits, etc.)

---

## Luup Engineering Integration Points

| Where in App | API Call |
|---|---|
| "Share & Earn" button tap | `POST /api/referrals/generate` |
| Referral link display/share | Use `referralLink` from response |
| After first ride completes | `POST /api/conversions` |
| Rewards / Wallet screen | `GET /api/rewards/users/{id}` |
| Coupon apply at checkout | Use `couponCode` from rewards |

---

## Quick Reference

| | |
|---|---|
| **Base URL** | `http://localhost:8080` |
| **Auth Header** | `Authorization: ApiKey <key>` |
| **Public Endpoint** | `GET /r/{code}` (no auth needed) |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |

---

## Complete Flow Example

```
1. Register Luup → get API key
        ↓
2. Create campaign (rules, reward, trigger event)
        ↓
3. Existing user Sara requests her referral link
        ↓
4. Sara shares link → friend Karim clicks it (auto-redirect + click tracked)
        ↓
5. Karim completes first ride → Luup backend POSTs to /api/conversions
        ↓
6. Platform auto-issues coupon codes to Sara AND Karim
        ↓
7. Luup fetches rewards via /api/rewards/users/{userId} to display in-app
```
