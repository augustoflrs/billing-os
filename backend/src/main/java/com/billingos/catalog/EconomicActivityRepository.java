package com.billingos.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EconomicActivityRepository extends JpaRepository<EconomicActivity, String> {
    List<EconomicActivity> findByActiveTrueOrderByName();
}
