package com.billingos.company;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class CompanyDto {

    /** Request to create or update a company */
    public record Request(
            @NotBlank @Size(max = 255)
            String legalName,

            @Size(max = 255)
            String tradeName,

            @NotBlank @Size(max = 20)
            @Pattern(regexp = "\\d{4}-\\d{6}-\\d{3}-\\d", message = "NIT must follow format 0000-000000-000-0")
            String nit,

            @Size(max = 20)
            String nrc,

            /** Economic activity code from catalog */
            @Size(max = 20)
            String economicActivityCode,

            @Size(max = 255)
            String email,

            @Size(max = 50)
            String phone
    ) {}

    /** Full company response */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Response(
            String id,
            String legalName,
            String tradeName,
            String nit,
            String nrc,
            String economicActivityCode,
            String economicActivityName,
            String email,
            String phone,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Long version
    ) {}

    /** Minimal reference used in nested responses */
    public record Ref(String id, String legalName, String nit) {}
}
