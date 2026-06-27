package com.billingos.catalog;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "dte_document_type", schema = "invoicing")
@Getter
public class DteDocumentType {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
