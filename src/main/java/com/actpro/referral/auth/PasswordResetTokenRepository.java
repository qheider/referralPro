package com.actpro.referral.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Query("""
            SELECT t
            FROM PasswordResetToken t
            WHERE t.dashboardUser.id = :dashboardUserId
              AND t.usedAt IS NULL
              AND t.revokedAt IS NULL
            """)
    List<PasswordResetToken> findActiveByDashboardUserId(@Param("dashboardUserId") Long dashboardUserId);
}
