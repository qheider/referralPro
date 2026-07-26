-- Add comprehensive company registration fields
ALTER TABLE companies
    ADD COLUMN website VARCHAR(255) AFTER email,
    ADD COLUMN industry VARCHAR(100) AFTER website,
    ADD COLUMN country VARCHAR(100) AFTER industry,
    ADD COLUMN tax_id VARCHAR(100) AFTER country,
    ADD COLUMN company_size VARCHAR(50) AFTER tax_id,
    ADD COLUMN preferred_currency VARCHAR(10) AFTER company_size,
    ADD COLUMN company_logo_url VARCHAR(500) AFTER preferred_currency,
    ADD COLUMN description VARCHAR(1000) AFTER company_logo_url,
    
    -- Address fields
    ADD COLUMN address_street VARCHAR(255) AFTER description,
    ADD COLUMN address_city VARCHAR(100) AFTER address_street,
    ADD COLUMN address_state VARCHAR(100) AFTER address_city,
    ADD COLUMN address_zip VARCHAR(20) AFTER address_state,
    
    -- Social links
    ADD COLUMN linkedin_url VARCHAR(500) AFTER address_zip,
    ADD COLUMN twitter_url VARCHAR(500) AFTER linkedin_url,
    ADD COLUMN facebook_url VARCHAR(500) AFTER twitter_url,
    
    -- Registration & Compliance
    ADD COLUMN registration_certificate_url VARCHAR(500) AFTER facebook_url,
    ADD COLUMN gst_vat_number VARCHAR(100) AFTER registration_certificate_url,
    ADD COLUMN industry_license_number VARCHAR(100) AFTER gst_vat_number,
    ADD COLUMN dpa_accepted BOOLEAN DEFAULT FALSE AFTER industry_license_number,
    
    -- Contact details
    ADD COLUMN secondary_contact_name VARCHAR(255) AFTER dpa_accepted,
    ADD COLUMN department VARCHAR(100) AFTER secondary_contact_name,
    ADD COLUMN referral_program_manager VARCHAR(255) AFTER department,
    
    -- Business preferences
    ADD COLUMN estimated_monthly_volume INT AFTER referral_program_manager,
    ADD COLUMN reward_type_preference VARCHAR(50) AFTER estimated_monthly_volume,
    ADD COLUMN referral_source VARCHAR(255) AFTER reward_type_preference,
    ADD COLUMN signup_referral_code VARCHAR(100) AFTER referral_source;
