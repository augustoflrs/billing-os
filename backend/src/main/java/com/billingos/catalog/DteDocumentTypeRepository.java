package com.billingos.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DteDocumentTypeRepository extends JpaRepository<DteDocumentType, String> {
    List<DteDocumentType> findByActiveTrueOrderByCode();
}
