package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.RateCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateCardRepository extends JpaRepository<RateCard, UUID> {

    /**
     * Find the best-match published rate for a given service, zone, billed weight,
     * and billing date. Returns the most recently effective row.
     */
    @Query("""
        SELECT r FROM RateCard r
        WHERE r.serviceCode = :serviceCode
          AND r.zone = :zone
          AND r.weightFromLb <= :weight
          AND r.weightToLb >= :weight
          AND r.effectiveFrom <= :billDate
          AND (r.effectiveTo IS NULL OR r.effectiveTo >= :billDate)
        ORDER BY r.effectiveFrom DESC
        """)
    List<RateCard> findMatchingRates(
        @Param("serviceCode") String serviceCode,
        @Param("zone") Integer zone,
        @Param("weight") BigDecimal weight,
        @Param("billDate") LocalDate billDate
    );

    default Optional<RateCard> findRate(String serviceCode, Integer zone, BigDecimal weight, LocalDate billDate) {
        List<RateCard> results = findMatchingRates(serviceCode, zone, weight, billDate);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // Latest version string for audit/response
    @Query("SELECT DISTINCT r.version FROM RateCard r ORDER BY r.version DESC")
    List<String> findAllVersions();
}
