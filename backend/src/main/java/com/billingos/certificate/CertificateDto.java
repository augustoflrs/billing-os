package com.billingos.certificate;

import java.time.OffsetDateTime;

public class CertificateDto {

    public record Response(
            String id,
            String companyId,
            String alias,
            String certificatePath,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            boolean active
    ) {}
}
