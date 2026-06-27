package com.billingos.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerTaxProfileRepository extends JpaRepository<CustomerTaxProfile, String> {
    Optional<CustomerTaxProfile> findByCustomer_Id(String customerId);
}
