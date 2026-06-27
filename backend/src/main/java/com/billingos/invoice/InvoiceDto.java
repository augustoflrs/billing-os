package com.billingos.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class InvoiceDto {

    // ── Requests ────────────────────────────────────────────────

    public record CreateInvoiceRequest(
            @NotBlank String customerId,
            @NotBlank String pointOfSaleId,
            @NotBlank String documentTypeCode,
            Instant invoiceDate,
            @NotEmpty @Valid List<LineRequest> lines
    ) {}

    public record LineRequest(
            String billableItemId,
            String itemName,
            String itemDescription,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @Positive BigDecimal unitPrice,
            BigDecimal discountAmount,
            String taxCode
    ) {}

    // ── Responses ────────────────────────────────────────────────

    public record InvoiceResponse(
            String id,
            String invoiceNumber,
            String customerId,
            String customerName,
            String pointOfSaleId,
            String documentTypeCode,
            Instant invoiceDate,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal balanceAmount,
            String statusCode,
            String statusName,
            List<LineResponse> lines
    ) {}

    public record LineResponse(
            String id,
            String billableItemId,
            String itemName,
            String itemDescription,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            List<TaxResponse> taxes
    ) {}

    public record TaxResponse(
            String taxCode,
            BigDecimal rate,
            BigDecimal taxableAmount,
            BigDecimal taxAmount
    ) {}

    public record InvoicePageResponse(
            List<InvoiceSummary> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record InvoiceSummary(
            String id,
            String invoiceNumber,
            String customerId,
            String customerName,
            String documentTypeCode,
            Instant invoiceDate,
            BigDecimal totalAmount,
            BigDecimal balanceAmount,
            String statusCode,
            String statusName
    ) {}
}
