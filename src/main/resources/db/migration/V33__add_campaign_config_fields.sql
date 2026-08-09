-- Campaign configuration fields the admin flow is expected to capture alongside reward rules
-- (qualifying conditions, incentive description, terms, and a budget cap). All nullable/additive:
-- captured/displayed only for now, no enforcement (e.g. budget isn't checked against spend) - see
-- CampaignService/CreateCampaignRequest.

ALTER TABLE campaigns
    ADD COLUMN qualifying_conditions TEXT NULL AFTER description,
    ADD COLUMN incentive_description TEXT NULL AFTER qualifying_conditions,
    ADD COLUMN terms_url VARCHAR(500) NULL AFTER incentive_description,
    ADD COLUMN budget_cap DECIMAL(12, 2) NULL AFTER terms_url;
