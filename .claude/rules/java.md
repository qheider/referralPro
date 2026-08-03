---
paths:
  - "src/main/java/**/*.java"
---

# Java / Spring Boot conventions

- Organize by feature (`company`, `campaign`, `user`, `referral`, `click`, `conversion`, `reward`, `auth`, `ambassador`, `dashboard`, `security`, `common`, `config`), not by layer.
- Controllers stay thin and delegate to services; business rules live in services (`ReferralService`, `ConversionService`, `RewardService`, `CampaignService`, `AmbassadorAdminService`, `AmbassadorPortalService`, `CampaignAssignmentService`).
- Success responses use `common/ApiResponse<T>`; errors are shaped by `common/GlobalExceptionHandler` into `common/ErrorResponse`, backed by `common/exception/{BadRequestException,NotFoundException,UnauthorizedException}`.
- Request/response DTOs are Java records, under each feature's `dto/` package.
- Entities extend `common/BaseEntity` for `id`/`created_at`/`updated_at`; auditing is enabled in `config/JpaConfig.java` — don't redeclare audit fields.
- Dashboard analytics (`dashboard/DashboardService.java`) use `EntityManager` with native SQL, not derived repository queries — follow that pattern for new cross-entity analytics rather than composing JPA repository methods.
