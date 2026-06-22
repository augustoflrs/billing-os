package com.billingos.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointOfSaleRepository extends JpaRepository<PointOfSale, String> {
    List<PointOfSale> findByBranchIdAndActiveTrueOrderByCodeAsc(String branchId);
    boolean existsByBranchIdAndCode(String branchId, String code);
    boolean existsByBranchIdAndCodeAndIdNot(String branchId, String code, String id);
}
