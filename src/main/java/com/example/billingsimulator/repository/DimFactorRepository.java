package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.DimFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DimFactorRepository extends JpaRepository<DimFactor, String> {
    // findById(serviceCode) covers all needed lookups
}
