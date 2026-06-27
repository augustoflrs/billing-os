package com.billingos.invoice;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.pointOfSaleId = :posId AND s.documentTypeCode = :docType")
    Optional<InvoiceSequence> findByPosAndDocTypeLocked(
            @Param("posId") String posId,
            @Param("docType") String docType);
}
