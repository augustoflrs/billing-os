package com.billingos.company;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<CompanyDto.Response> getCompany() {
        return ResponseEntity.ok(companyService.getCompany());
    }

    @PostMapping
    public ResponseEntity<CompanyDto.Response> createCompany(
            @Valid @RequestBody CompanyDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createCompany(request));
    }

    /**
     * Update requires the current version for optimistic locking.
     * Pass version in X-Expected-Version header.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CompanyDto.Response> updateCompany(
            @PathVariable String id,
            @Valid @RequestBody CompanyDto.Request request,
            @RequestHeader("X-Expected-Version") Long expectedVersion) {
        return ResponseEntity.ok(companyService.updateCompany(id, request, expectedVersion));
    }
}
