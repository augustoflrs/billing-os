package com.billingos.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    @Query("""
            SELECT c FROM Customer c
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(c.legalName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR c.customerNumber LIKE CONCAT('%', :search, '%'))
            ORDER BY c.legalName ASC
            """)
    Page<Customer> search(@Param("search") String search, Pageable pageable);

    boolean existsByCustomerNumber(String customerNumber);
}
