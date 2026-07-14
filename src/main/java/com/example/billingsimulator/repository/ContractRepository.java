package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, String> {

    List<Contract> findByCompanyId(UUID companyId);
}
