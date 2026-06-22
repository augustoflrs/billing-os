package com.billingos.company;

import com.billingos.catalog.EconomicActivity;
import com.billingos.catalog.EconomicActivityRepository;
import com.billingos.common.UlidGenerator;
import com.billingos.common.exception.ConflictException;
import com.billingos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final EconomicActivityRepository economicActivityRepository;

    @Transactional(readOnly = true)
    public CompanyDto.Response getCompany() {
        Company company = companyRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new ResourceNotFoundException("No company configured yet"));
        return toResponse(company);
    }

    @Transactional
    public CompanyDto.Response createCompany(CompanyDto.Request request) {
        if (companyRepository.findFirstByActiveTrueOrderByCreatedAtAsc().isPresent()) {
            throw new ConflictException("A company already exists. Use PUT to update it.");
        }
        if (companyRepository.existsByNit(request.nit())) {
            throw new ConflictException("NIT " + request.nit() + " is already registered");
        }

        Company company = new Company();
        company.setId(UlidGenerator.generate());
        company.setCreatedBy(currentUser());
        applyRequest(company, request);
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyDto.Response updateCompany(String id, CompanyDto.Request request, Long expectedVersion) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));

        if (!company.getVersion().equals(expectedVersion)) {
            throw new ConflictException("Stale version: expected " + expectedVersion
                    + " but found " + company.getVersion() + ". Reload and retry.");
        }
        if (!company.getNit().equals(request.nit()) && companyRepository.existsByNit(request.nit())) {
            throw new ConflictException("NIT " + request.nit() + " is already registered");
        }

        applyRequest(company, request);
        company.setUpdatedAt(OffsetDateTime.now());
        company.setUpdatedBy(currentUser());
        return toResponse(companyRepository.save(company));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private void applyRequest(Company company, CompanyDto.Request req) {
        company.setLegalName(req.legalName());
        company.setTradeName(req.tradeName());
        company.setNit(req.nit());
        company.setNrc(req.nrc());
        company.setEmail(req.email());
        company.setPhone(req.phone());

        if (req.economicActivityCode() != null && !req.economicActivityCode().isBlank()) {
            EconomicActivity ea = economicActivityRepository.findById(req.economicActivityCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Economic activity not found: " + req.economicActivityCode()));
            company.setEconomicActivity(ea);
        } else {
            company.setEconomicActivity(null);
        }
    }

    CompanyDto.Response toResponse(Company c) {
        return new CompanyDto.Response(
                c.getId(),
                c.getLegalName(),
                c.getTradeName(),
                c.getNit(),
                c.getNrc(),
                c.getEconomicActivity() != null ? c.getEconomicActivity().getCode() : null,
                c.getEconomicActivity() != null ? c.getEconomicActivity().getName() : null,
                c.getEmail(),
                c.getPhone(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getVersion()
        );
    }

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
