package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.ZoneMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZoneMatrixRepository extends JpaRepository<ZoneMatrix, Long> {

    Optional<ZoneMatrix> findByOriginPrefixAndDestPrefix(String originPrefix, String destPrefix);
}
