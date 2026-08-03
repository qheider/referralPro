-- Second step of the API-key hashing migration (see V20). Separated so V20's backfill into
-- company_api_keys can be verified before this irreversible drop of the plaintext column runs.
ALTER TABLE companies
    DROP INDEX uk_companies_api_key,
    DROP COLUMN api_key;
