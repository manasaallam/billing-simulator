package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.MinShippingCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MinShippingChargeRepository extends JpaRepository<MinShippingCharge, Long> {

    Optional<MinShippingCharge> findByServiceCode(String serviceCode);
}
