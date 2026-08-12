package com.actpro.referral.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountInvitationRepository extends JpaRepository<AccountInvitation, Long> {

    Optional<AccountInvitation> findByTokenHash(String tokenHash);

    @Query("""
            SELECT i
            FROM AccountInvitation i
            WHERE i.dashboardUser.id = :dashboardUserId
              AND i.acceptedAt IS NULL
              AND i.revokedAt IS NULL
            """)
    List<AccountInvitation> findActiveByDashboardUserId(@Param("dashboardUserId") Long dashboardUserId);
}
