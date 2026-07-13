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

    List<BaselineSnapshot> findByCompanyIdOrderByPeriodToDesc(UUID companyId);

    /** Latest baseline for a company (most recent period end). */
    @Query("""
        SELECT b FROM BaselineSnapshot b
        WHERE b.companyId = :companyId
        ORDER BY b.periodTo DESC
        """)
    List<BaselineSnapshot> findLatestForCompany(@Param("companyId") UUID companyId);

    default Optional<BaselineSnapshot> findLatest(UUID companyId) {
        List<BaselineSnapshot> results = findLatestForCompany(companyId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
