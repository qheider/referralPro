package com.actpro.referral.ambassador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmbassadorProfileRepository extends JpaRepository<AmbassadorProfile, Long> {

    @Query("""
            SELECT ap
            FROM AmbassadorProfile ap
            JOIN FETCH ap.user u
            JOIN FETCH ap.company c
            WHERE ap.id = :id
              AND ap.company.id = :companyId
            """)
    Optional<AmbassadorProfile> findDetailedByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    @Query(
            value = """
                    SELECT ap
                    FROM AmbassadorProfile ap
                    JOIN ap.user u
                    WHERE ap.company.id = :companyId
                      AND (:status IS NULL OR ap.status = :status)
                      AND (
                           :search IS NULL
                           OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(ap.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(ap)
                    FROM AmbassadorProfile ap
                    JOIN ap.user u
                    WHERE ap.company.id = :companyId
                      AND (:status IS NULL OR ap.status = :status)
                      AND (
                           :search IS NULL
                           OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(COALESCE(ap.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                      )
                    """
    )
    Page<AmbassadorProfile> searchByCompanyId(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("status") AmbassadorStatus status,
            Pageable pageable
    );

    Optional<AmbassadorProfile> findByUserId(Long userId);

    Optional<AmbassadorProfile> findByIdAndCompanyId(Long id, Long companyId);

    Optional<AmbassadorProfile> findByCompanyIdAndUserId(Long companyId, Long userId);

    @Query("""
            SELECT ap
            FROM AmbassadorProfile ap
            JOIN FETCH ap.user u
            JOIN FETCH ap.company c
            WHERE ap.company.id = :companyId
              AND ap.user.id = :userId
            """)
    Optional<AmbassadorProfile> findDetailedByCompanyIdAndUserId(@Param("companyId") Long companyId, @Param("userId") Long userId);

    boolean existsByAmbassadorCode(String ambassadorCode);
}
