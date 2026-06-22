package com.billingos.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final EconomicActivityRepository economicActivityRepository;

    record EconomicActivityDto(String code, String name) {}

    @GetMapping("/economic-activities")
    public List<EconomicActivityDto> getEconomicActivities() {
        return economicActivityRepository.findByActiveTrueOrderByName()
                .stream()
                .map(ea -> new EconomicActivityDto(ea.getCode(), ea.getName()))
                .toList();
    }
}
