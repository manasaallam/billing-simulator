package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.ServiceLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceLevelRepository extends JpaRepository<ServiceLevel, String> {
    // findById(serviceCode) covers all lookups
}
