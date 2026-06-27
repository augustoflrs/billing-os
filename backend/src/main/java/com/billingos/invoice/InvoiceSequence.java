package com.billingos.invoice;

import com.billingos.common.UlidGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoice_sequence", schema = "invoicing")
@Getter
@Setter
public class InvoiceSequence {

    @Id
    private String id = UlidGenerator.generate();

    @Column(name = "point_of_sale_id", nullable = false, length = 26)
    private String pointOfSaleId;

    @Column(name = "document_type_code", nullable = false, length = 10)
    private String documentTypeCode;

    @Column(name = "current_value", nullable = false)
    private long currentValue = 0;
}
