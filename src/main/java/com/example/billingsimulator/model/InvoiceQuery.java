package com.example.billingsimulator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Customer's question about their invoice.
 *
 * Examples:
 *   "Why was I charged $20.87 for tracking 1Z999?"
 *   "Why is my fuel surcharge so high this month?"
 *   "What is the Delivery Area Surcharge?"
 *   "Which discount was applied to my Ground shipments?"
 */
public class InvoiceQuery {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Please ask a question about your invoice")
    @Size(min = 5, max = 2000)
    private String question;

    private String invoiceId;       // Optional: specific invoice to explain
    private String trackingNumber;  // Optional: specific shipment to explain

    public InvoiceQuery() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
}
