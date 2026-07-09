package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCustomerName(String customerName);
    List<Invoice> findByStatus(String status);
}
