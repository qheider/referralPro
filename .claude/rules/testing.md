---
paths:
  - "src/test/**/*.java"
---

# Testing conventions

- Real tests live under `src/test/java/com/actpro/referral/...` (e.g. `security/CurrentUserServiceTest`, `ambassador/AmbassadorAdminServiceTest`, `ambassador/CampaignAssignmentServiceTest`, `campaign/CampaignControllerSecurityTest`) and use Mockito rather than a full Spring context.
- `src/test/resources/application-test.yml` backs any test that does need to boot a context (H2 in-memory DB is on the classpath for this) — only reach for a full context when mocking collaborators genuinely isn't enough.
- `src/test/java/ai/actpro/referralPro/ReferralProApplicationTests.java` is a broken smoke test: it's packaged under `ai.actpro.referralPro` but the application class is `com.actpro.referral.ReferralApplication`, so it never discovers a `@SpringBootConfiguration` and will fail/be skipped. Don't treat it as coverage, and don't "fix" it by moving real tests into that package.
- Run a single class: `.\mvnw.cmd -Dtest=CurrentUserServiceTest test`; a single method: `.\mvnw.cmd -Dtest=CurrentUserServiceTest#shouldRejectCrossCompanyAccess test`.
- Name test methods for the behavior under test (`shouldRejectCrossCompanyAccess`), and cover the negative/denied case explicitly for anything tenant- or role-scoped, not just the happy path.
