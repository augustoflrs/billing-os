package com.billingos.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findByCompanyIdAndActiveTrueOrderByCodeAsc(String companyId);
    boolean existsByCompanyIdAndCode(String companyId, String code);
    boolean existsByCompanyIdAndCodeAndIdNot(String companyId, String code, String id);
}
