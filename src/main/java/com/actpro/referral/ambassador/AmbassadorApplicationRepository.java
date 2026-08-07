package com.actpro.referral.ambassador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmbassadorApplicationRepository extends JpaRepository<AmbassadorApplication, Long> {

    Optional<AmbassadorApplication> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndEmailAndStatus(Long companyId, String email, ApplicationStatus status);

    @Query(
            value = """
                    SELECT a
                    FROM AmbassadorApplication a
                    WHERE a.company.id = :companyId
                      AND (:status IS NULL OR a.status = :status)
                      AND (
                           :search IS NULL
                           OR LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(a.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(a)
                    FROM AmbassadorApplication a
                    WHERE a.company.id = :companyId
                      AND (:status IS NULL OR a.status = :status)
                      AND (
                           :search IS NULL
                           OR LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(a.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """
    )
    Page<AmbassadorApplication> searchByCompanyId(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("status") ApplicationStatus status,
            Pageable pageable
    );
}
