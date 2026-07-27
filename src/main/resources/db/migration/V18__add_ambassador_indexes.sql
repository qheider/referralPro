ALTER TABLE referral_clicks
    ADD INDEX idx_referral_clicks_company_id (company_id),
    ADD INDEX idx_referral_clicks_campaign_id (campaign_id),
    ADD INDEX idx_referral_clicks_referral_link_id (referral_link_id),
    ADD INDEX idx_referral_clicks_ambassador_user_id (ambassador_user_id),
    ADD INDEX idx_referral_clicks_session_id (session_id);

ALTER TABLE referrals
    ADD INDEX idx_referrals_ambassador_user_id (ambassador_user_id),
    ADD INDEX idx_referrals_referral_link_id (referral_link_id),
    ADD INDEX idx_referrals_customer_user_id (customer_user_id),
    ADD INDEX idx_referrals_registered_at (registered_at),
    ADD INDEX idx_referrals_converted_at (converted_at);
