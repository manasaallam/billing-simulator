package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.BaselineSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BaselineSnapshotRepository extends JpaRepository<BaselineSnapshot, UUID> {

    List<BaselineSnapshot> findByAccountIdOrderByPeriodToDesc(UUID accountId);

    /** Latest baseline for an account (most recent period end). */
    @Query("""
        SELECT b FROM BaselineSnapshot b
        WHERE b.accountId = :accountId
        ORDER BY b.periodTo DESC
        """)
    List<BaselineSnapshot> findLatestForAccount(@Param("accountId") UUID accountId);

    default Optional<BaselineSnapshot> findLatest(UUID accountId) {
        List<BaselineSnapshot> results = findLatestForAccount(accountId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
