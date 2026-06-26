package com.actpro.referral.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardUserRepository extends JpaRepository<DashboardUser, Long> {

    @Query("SELECT u FROM DashboardUser u LEFT JOIN FETCH u.company WHERE u.username = :username")
    Optional<DashboardUser> findByUsernameWithCompany(String username);

    Optional<DashboardUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
