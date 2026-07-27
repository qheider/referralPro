ALTER TABLE referrals
    ADD COLUMN ambassador_user_id BIGINT NULL AFTER referrer_user_id,
    ADD COLUMN referral_link_id BIGINT NULL AFTER ambassador_user_id,
    ADD COLUMN customer_user_id BIGINT NULL AFTER referral_link_id,
    ADD COLUMN registered_at TIMESTAMP NULL AFTER status,
    ADD COLUMN converted_at TIMESTAMP NULL AFTER registered_at,
    ADD COLUMN booking_id VARCHAR(100) NULL AFTER converted_at,
    ADD COLUMN rental_id VARCHAR(100) NULL AFTER booking_id,
    ADD COLUMN discount_amount DECIMAL(10, 2) NULL AFTER rental_id,
    ADD COLUMN currency VARCHAR(10) NULL AFTER discount_amount;

ALTER TABLE referrals
    ADD CONSTRAINT fk_referrals_ambassador_user FOREIGN KEY (ambassador_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_referrals_referral_link FOREIGN KEY (referral_link_id) REFERENCES referral_links(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_referrals_customer_user FOREIGN KEY (customer_user_id) REFERENCES platform_users(id) ON DELETE SET NULL;
