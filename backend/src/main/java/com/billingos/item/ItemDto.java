package com.billingos.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class ItemDto {

    public record PriceInput(
            @NotNull @DecimalMin("0.0001") BigDecimal unitPrice,
            @NotNull OffsetDateTime validFrom,
            OffsetDateTime validTo
    ) {}

    public record Request(
            @NotBlank @Size(max = 20) String itemType,
            @Size(max = 100) String sku,
            @Size(max = 100) String code,
            @NotBlank @Size(max = 255) String name,
            String description,
            @Valid PriceInput price
    ) {}

    public record PriceResponse(
            String id,
            BigDecimal unitPrice,
            String currencyCode,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            boolean active
    ) {}

    public record Response(
            String id,
            String itemType,
            String sku,
            String code,
            String name,
            String description,
            boolean active,
            Long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<PriceResponse> prices,
            PriceResponse currentPrice
    ) {}

    public record PageResponse(
            List<Response> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
