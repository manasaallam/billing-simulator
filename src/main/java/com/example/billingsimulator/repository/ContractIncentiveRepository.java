package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.ContractIncentive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractIncentiveRepository extends JpaRepository<ContractIncentive, Long> {

    List<ContractIncentive> findByContractId(String contractId);
}
