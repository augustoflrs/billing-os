package com.billingos.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor
public class CatalogController {

    private final EconomicActivityRepository economicActivityRepository;
    private final DepartmentRepository departmentRepository;
    private final MunicipalityRepository municipalityRepository;
    private final TaxDefinitionRepository taxDefinitionRepository;
    private final DteDocumentTypeRepository dteDocumentTypeRepository;

    record EconomicActivityDto(String code, String name) {}
    record DepartmentDto(String code, String name) {}
    record MunicipalityDto(String code, String departmentCode, String name) {}
    record TaxDefinitionDto(String code, String name, BigDecimal rate) {}
    record DocumentTypeDto(String code, String name) {}

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

    @GetMapping("/tax-definitions")
    public List<TaxDefinitionDto> getTaxDefinitions() {
        return taxDefinitionRepository.findByActiveTrueOrderByName()
                .stream()
                .map(t -> new TaxDefinitionDto(t.getCode(), t.getName(), t.getRate()))
                .toList();
    }

    @GetMapping("/document-types")
    public List<DocumentTypeDto> getDocumentTypes() {
        return dteDocumentTypeRepository.findByActiveTrueOrderByCode()
                .stream()
                .map(d -> new DocumentTypeDto(d.getCode(), d.getName()))
                .toList();
    }
}
