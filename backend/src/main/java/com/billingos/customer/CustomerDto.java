package com.billingos.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class CustomerDto {

    public record AddressInput(
            @NotBlank @Size(max = 255) String addressLine1,
            @Size(max = 10) String departmentCode,
            @Size(max = 10) String municipalityCode
    ) {}

    public record TaxProfileInput(
            @NotBlank @Size(max = 20) String documentType,
            @NotBlank @Size(max = 50) String documentNumber,
            @Size(max = 20) String nit,
            @Size(max = 20) String nrc,
            @Size(max = 20) String economicActivityCode
    ) {}

    public record Request(
            @NotBlank @Size(max = 255) String legalName,
            @Size(max = 255) String tradeName,
            @Email @Size(max = 255) String email,
            @Size(max = 50) String phone,
            @Valid AddressInput address,
            @Valid TaxProfileInput taxProfile
    ) {}

    public record AddressResponse(
            String id,
            String addressLine1,
            String departmentCode,
            String municipalityCode,
            boolean isDefault
    ) {}

    public record TaxProfileResponse(
            String id,
            String documentType,
            String documentNumber,
            String nit,
            String nrc,
            String economicActivityCode
    ) {}

    public record Response(
            String id,
            String customerNumber,
            String legalName,
            String tradeName,
            String email,
            String phone,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long version,
            AddressResponse address,
            TaxProfileResponse taxProfile
    ) {}

    public record PageResponse(
            java.util.List<Response> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
