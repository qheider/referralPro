# Hosted dashboard CORS

The dashboard at `https://referral.luupnow.ca` calls the API configured in
`referralPro-dashboard/src/environments/environment.prod.ts` (currently
`https://api-uat.referral.actpro.ai/api`). Registration sends JSON, so the browser
first sends an OPTIONS preflight to `/api/companies/register`.

The live API returned HTTP 403 with `Invalid CORS request` for that origin during
investigation. The previous default backend allowlist contained only local/private
HTTP origins. Making registration public in Spring Security does not bypass CORS.

The default allowlist now includes the exact HTTPS dashboard origin. Deploy the
updated backend to activate it. If Elastic Beanstalk has `CORS_ALLOWED_ORIGINS`
configured, update that environment property to include
`https://referral.luupnow.ca`: the property replaces, rather than extends, the
defaults. Retain any other required origins in the comma-separated value. Do not
include `/register`, a trailing slash, or use `*` for the hosted deployment.

The UAT deployment workflow now reads the existing Elastic Beanstalk CORS
environment property, preserves its entries, and adds the dashboard origin in
the same update as the application deployment. This fixes an existing environment
override even when changing the application's default had no effect. It then
checks the public API preflight and fails the workflow if the required CORS
headers are missing. The deployment AWS identity needs
`elasticbeanstalk:DescribeConfigurationSettings` as well as its existing
deployment permissions.

To repair the running environment without rebuilding, add the dashboard origin
to `CORS_ALLOWED_ORIGINS` in Elastic Beanstalk's environment properties and apply
the change. This setting is consumed by the existing backend. Preserve any other
required origins. Changing a local `.env` file does not update Elastic Beanstalk.

For a deployment serving only this dashboard:

```text
CORS_ALLOWED_ORIGINS=https://referral.luupnow.ca
APP_FRONTEND_URL=https://referral.luupnow.ca
```

`APP_FRONTEND_URL` controls campaign join links and generated email links; it does not configure CORS.
The UAT deployment workflow sets this property to `https://referral.luupnow.ca`,
replacing any old domain configured in Elastic Beanstalk. Existing campaigns
return corrected join links after the environment update; no campaign recreation is needed.
Apply environment changes/restart the backend, then verify from PowerShell:

```powershell
curl.exe -i -X OPTIONS https://api-uat.referral.actpro.ai/api/companies/register -H "Origin: https://referral.luupnow.ca" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type"
```

Expect HTTP 200, `Access-Control-Allow-Origin: https://referral.luupnow.ca`, and
POST in `Access-Control-Allow-Methods`. Repeat with an unrelated origin and
expect 403. Then retry registration in the browser. No frontend rebuild is needed
for this CORS fix.
