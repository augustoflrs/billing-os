package com.billingos.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, String> {
    Optional<Company> findFirstByActiveTrueOrderByCreatedAtAsc();
    boolean existsByNit(String nit);
}
