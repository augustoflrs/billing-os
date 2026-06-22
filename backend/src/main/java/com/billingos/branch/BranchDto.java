package com.billingos.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BranchDto {

    public record Request(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 255) String addressLine1,
            @Size(max = 10) String departmentCode,
            @Size(max = 10) String municipalityCode,
            @Size(max = 50) String phone
    ) {}

    public record Response(
            String id,
            String companyId,
            String code,
            String name,
            String addressLine1,
            String departmentCode,
            String municipalityCode,
            String phone,
            boolean active
    ) {}
}
