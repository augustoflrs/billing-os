package com.billingos.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxDefinitionRepository extends JpaRepository<TaxDefinition, String> {
    List<TaxDefinition> findByActiveTrueOrderByName();
}
