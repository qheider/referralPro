package com.actpro.referral.click;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralClickRepository extends JpaRepository<ReferralClick, Long> {
}
