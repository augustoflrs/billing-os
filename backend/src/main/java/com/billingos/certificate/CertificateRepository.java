package com.billingos.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, String> {
    List<Certificate> findByCompanyIdOrderByValidToDesc(String companyId);
    Optional<Certificate> findFirstByCompanyIdAndActiveTrueOrderByValidToDesc(String companyId);
}
