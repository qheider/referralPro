-- Ambassador Applications (phase 7): public self-service "apply to become an ambassador"
-- submissions, reviewed by a company admin into approve/reject. Approval provisions a
-- DashboardUser + AmbassadorProfile via AmbassadorAdminService.provisionAmbassadorAccount and
-- issues an invitation token through the existing account_invitations flow (phase 4).
--
-- No DB-level uniqueness on (company_id, email): MySQL has no partial unique index, and a plain
-- unique constraint would incorrectly block resubmission after a REJECTED application. Duplicate
-- pending-application prevention is enforced in AmbassadorApplicationService at the app level
-- instead (idx_ambassador_applications_company_email exists to make that check fast, not to
-- enforce uniqueness).
CREATE TABLE ambassador_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NULL,
    display_name VARCHAR(255) NULL,
    bio TEXT NULL,
    social_media_platform VARCHAR(100) NULL,
    social_media_handle VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500) NULL,
    reviewed_by_user_id BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    resulting_ambassador_profile_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ambassador_applications_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_ambassador_applications_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,
    CONSTRAINT fk_ambassador_applications_profile FOREIGN KEY (resulting_ambassador_profile_id) REFERENCES ambassador_profiles(id) ON DELETE SET NULL,
    INDEX idx_ambassador_applications_company_id (company_id),
    INDEX idx_ambassador_applications_company_status (company_id, status),
    INDEX idx_ambassador_applications_company_email (company_id, email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
