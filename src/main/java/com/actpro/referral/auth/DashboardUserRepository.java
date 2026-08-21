package com.actpro.referral.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardUserRepository extends JpaRepository<DashboardUser, Long> {

    @Query("SELECT u FROM DashboardUser u LEFT JOIN FETCH u.company WHERE u.username = :username")
    Optional<DashboardUser> findByUsernameWithCompany(String username);

    @Query("SELECT u FROM DashboardUser u LEFT JOIN FETCH u.company WHERE u.id = :id")
    Optional<DashboardUser> findByIdWithCompany(Long id);

    Optional<DashboardUser> findByUsername(String username);

    boolean existsByUsername(String username);

    // Used to notify every company admin (not just one) when a new ambassador application comes
    // in - see AmbassadorApplicationService.submitApplication.
    List<DashboardUser> findByCompanyIdAndRole(Long companyId, UserRole role);
}
