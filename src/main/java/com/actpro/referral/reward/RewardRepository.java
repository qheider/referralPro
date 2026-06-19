package com.actpro.referral.reward;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    boolean existsByCouponCode(String couponCode);

    @Query("SELECT r FROM Reward r " +
            "JOIN r.user u " +
            "WHERE r.company.id = :companyId " +
            "AND u.externalUserId = :externalUserId")
    List<Reward> findByCompanyIdAndUserExternalUserId(
            @Param("companyId") Long companyId,
            @Param("externalUserId") String externalUserId);
}
