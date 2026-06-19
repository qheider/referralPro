package com.actpro.referral.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByApiKey(String apiKey);

    Optional<Company> findByEmail(String email);

    boolean existsByEmail(String email);
}
