package com.billingos.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final EconomicActivityRepository economicActivityRepository;
    private final DepartmentRepository departmentRepository;
    private final MunicipalityRepository municipalityRepository;

    record EconomicActivityDto(String code, String name) {}
    record DepartmentDto(String code, String name) {}
    record MunicipalityDto(String code, String departmentCode, String name) {}

    @GetMapping("/economic-activities")
    public List<EconomicActivityDto> getEconomicActivities() {
        return economicActivityRepository.findByActiveTrueOrderByName()
                .stream()
                .map(ea -> new EconomicActivityDto(ea.getCode(), ea.getName()))
                .toList();
    }

    @GetMapping("/departments")
    public List<DepartmentDto> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc()
                .stream()
                .map(d -> new DepartmentDto(d.getCode(), d.getName()))
                .toList();
    }

    @GetMapping("/departments/{code}/municipalities")
    public List<MunicipalityDto> getMunicipalities(@PathVariable String code) {
        return municipalityRepository.findByDepartmentCodeOrderByNameAsc(code)
                .stream()
                .map(m -> new MunicipalityDto(m.getCode(), m.getDepartmentCode(), m.getName()))
                .toList();
    }
}
