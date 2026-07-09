package com.example.billingsimulator.service;

import com.example.billingsimulator.model.Invoice;
import com.example.billingsimulator.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    public Invoice createInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public Optional<Invoice> updateInvoice(Long id, Invoice invoiceDetails) {
        return invoiceRepository.findById(id).map(invoice -> {
            invoice.setCustomerName(invoiceDetails.getCustomerName());
            invoice.setAmount(invoiceDetails.getAmount());
            invoice.setStatus(invoiceDetails.getStatus());
            return invoiceRepository.save(invoice);
        });
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    public List<Invoice> getInvoicesByCustomer(String customerName) {
        return invoiceRepository.findByCustomerName(customerName);
    }

    public List<Invoice> getInvoicesByStatus(String status) {
        return invoiceRepository.findByStatus(status);
    }
}
