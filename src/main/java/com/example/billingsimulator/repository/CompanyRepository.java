package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Repository for the {@code company} table (mapped via {@link Account}). */
@Repository
public interface CompanyRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccessKey(String accessKey);
}
