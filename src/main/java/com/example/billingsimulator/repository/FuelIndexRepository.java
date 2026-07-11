package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.FuelIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuelIndexRepository extends JpaRepository<FuelIndex, Long> {

    Optional<FuelIndex> findByFuelProgramAndWeekCode(String fuelProgram, String weekCode);

    /**
     * Find the fuel rate in effect for a given billing date
     * (most recently effective row on or before that date).
     */
    @Query("""
        SELECT f FROM FuelIndex f
        WHERE f.fuelProgram = :fuelProgram
          AND f.effectiveFrom <= :billDate
        ORDER BY f.effectiveFrom DESC
        """)
    List<FuelIndex> findCurrentRates(
        @Param("fuelProgram") String fuelProgram,
        @Param("billDate") LocalDate billDate
    );

    default Optional<FuelIndex> findCurrentRate(String fuelProgram, LocalDate billDate) {
        List<FuelIndex> results = findCurrentRates(fuelProgram, billDate);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
