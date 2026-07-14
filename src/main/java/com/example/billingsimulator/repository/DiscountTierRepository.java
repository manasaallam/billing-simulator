package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.DiscountTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountTierRepository extends JpaRepository<DiscountTier, Long> {

    /**
     * Resolve the correct discount band for a program + category + weekly volume.
     * volMax IS NULL means open-ended top band (71+ shipments/week).
     */
    @Query("""
        SELECT d FROM DiscountTier d
        WHERE d.programId = :programId
          AND d.categoryCode = :categoryCode
          AND d.volMin <= :volume
          AND (d.volMax IS NULL OR d.volMax >= :volume)
        """)
    List<DiscountTier> findMatchingTiers(
        @Param("programId") UUID programId,
        @Param("categoryCode") String categoryCode,
        @Param("volume") int volume
    );

    default Optional<DiscountTier> findTier(UUID programId, String categoryCode, int volume) {
        List<DiscountTier> results = findMatchingTiers(programId, categoryCode, volume);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    // All tiers for a program — used by optimize scenario to enumerate bands
    List<DiscountTier> findByProgramIdOrderByVolMinAsc(UUID programId);
}
