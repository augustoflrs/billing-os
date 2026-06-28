package com.billingos.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class PaymentDto {

    public record AllocationRequest(
            @NotBlank String invoiceId,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {}

    public record CreatePaymentRequest(
            @NotBlank String paymentMethodCode,
            String referenceNumber,
            Instant paymentDate,
            @NotEmpty @Valid List<AllocationRequest> allocations
    ) {}

    public record AllocationResponse(
            String id,
            String invoiceId,
            String invoiceNumber,
            BigDecimal allocatedAmount
    ) {}

    public record PaymentResponse(
            String id,
            Instant paymentDate,
            BigDecimal amount,
            String paymentMethodCode,
            String referenceNumber,
            String statusCode,
            List<AllocationResponse> allocations
    ) {}
}
