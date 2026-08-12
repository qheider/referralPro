package com.actpro.referral.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // Atomic single-statement claim: MySQL takes row locks as it updates, so two dispatchers
    // running this concurrently can't claim the same row - the second one's WHERE (status =
    // 'PENDING') simply won't match rows the first already flipped to PROCESSING. Tags claimed
    // rows with a per-run token (rather than returning them directly, which UPDATE can't do) so
    // the caller can look up exactly what it claimed via findByLockedBy.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE outbox_events
            SET status = 'PROCESSING', locked_by = :claimToken, updated_at = CURRENT_TIMESTAMP
            WHERE status = 'PENDING' AND available_at <= :now
            ORDER BY available_at ASC
            LIMIT :batchSize
            """, nativeQuery = true)
    int claimBatch(@Param("claimToken") String claimToken, @Param("now") LocalDateTime now, @Param("batchSize") int batchSize);

    List<OutboxEvent> findByLockedBy(String lockedBy);
}
