package com.billingos.certificate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "certificate", schema = "invoicing")
@Getter
@Setter
public class Certificate {

    @Id
    private String id;

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(name = "certificate_path", nullable = false)
    private String certificatePath;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private OffsetDateTime validTo;

    @Column(nullable = false)
    private boolean active = true;
}
