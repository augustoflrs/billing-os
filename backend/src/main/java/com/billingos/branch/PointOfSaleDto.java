package com.billingos.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PointOfSaleDto {

    public record Request(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 255) String name
    ) {}

    public record Response(
            String id,
            String branchId,
            String code,
            String name,
            boolean active
    ) {}
}
