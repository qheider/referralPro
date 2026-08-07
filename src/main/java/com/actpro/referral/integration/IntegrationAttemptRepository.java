package com.actpro.referral.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationAttemptRepository extends JpaRepository<IntegrationAttempt, Long> {

    List<IntegrationAttempt> findByApiSubmissionIdOrderByAttemptNumberAsc(Long apiSubmissionId);
}
