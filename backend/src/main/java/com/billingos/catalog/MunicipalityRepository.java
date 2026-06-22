package com.billingos.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MunicipalityRepository extends JpaRepository<Municipality, String> {
    List<Municipality> findByDepartmentCodeOrderByNameAsc(String departmentCode);
}
