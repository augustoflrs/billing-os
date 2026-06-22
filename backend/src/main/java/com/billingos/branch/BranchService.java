package com.billingos.branch;

import com.billingos.common.UlidGenerator;
import com.billingos.common.exception.ConflictException;
import com.billingos.common.exception.ResourceNotFoundException;
import com.billingos.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final PointOfSaleRepository posRepository;
    private final CompanyRepository companyRepository;

    // ─── Branch ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BranchDto.Response> listBranches(String companyId) {
        requireCompany(companyId);
        return branchRepository.findByCompanyIdAndActiveTrueOrderByCodeAsc(companyId)
                .stream().map(this::toBranchResponse).toList();
    }

    @Transactional
    public BranchDto.Response createBranch(String companyId, BranchDto.Request req) {
        requireCompany(companyId);
        if (branchRepository.existsByCompanyIdAndCode(companyId, req.code())) {
            throw new ConflictException("Branch code '" + req.code() + "' already exists for this company");
        }
        Branch b = new Branch();
        b.setId(UlidGenerator.generate());
        b.setCompanyId(companyId);
        applyBranch(b, req);
        return toBranchResponse(branchRepository.save(b));
    }

    @Transactional
    public BranchDto.Response updateBranch(String companyId, String branchId, BranchDto.Request req) {
        Branch b = branchRepository.findById(branchId)
                .filter(x -> x.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        if (branchRepository.existsByCompanyIdAndCodeAndIdNot(companyId, req.code(), branchId)) {
            throw new ConflictException("Branch code '" + req.code() + "' already exists for this company");
        }
        applyBranch(b, req);
        return toBranchResponse(branchRepository.save(b));
    }

    @Transactional
    public void deactivateBranch(String companyId, String branchId) {
        Branch b = branchRepository.findById(branchId)
                .filter(x -> x.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
        b.setActive(false);
        branchRepository.save(b);
    }

    // ─── Point of Sale ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PointOfSaleDto.Response> listPointsOfSale(String companyId, String branchId) {
        requireBranch(companyId, branchId);
        return posRepository.findByBranchIdAndActiveTrueOrderByCodeAsc(branchId)
                .stream().map(this::toPosResponse).toList();
    }

    @Transactional
    public PointOfSaleDto.Response createPointOfSale(String companyId, String branchId, PointOfSaleDto.Request req) {
        requireBranch(companyId, branchId);
        if (posRepository.existsByBranchIdAndCode(branchId, req.code())) {
            throw new ConflictException("POS code '" + req.code() + "' already exists for this branch");
        }
        PointOfSale pos = new PointOfSale();
        pos.setId(UlidGenerator.generate());
        pos.setBranchId(branchId);
        pos.setCode(req.code());
        pos.setName(req.name());
        return toPosResponse(posRepository.save(pos));
    }

    @Transactional
    public PointOfSaleDto.Response updatePointOfSale(String companyId, String branchId, String posId, PointOfSaleDto.Request req) {
        requireBranch(companyId, branchId);
        PointOfSale pos = posRepository.findById(posId)
                .filter(x -> x.getBranchId().equals(branchId))
                .orElseThrow(() -> new ResourceNotFoundException("POS not found: " + posId));
        if (posRepository.existsByBranchIdAndCodeAndIdNot(branchId, req.code(), posId)) {
            throw new ConflictException("POS code '" + req.code() + "' already exists for this branch");
        }
        pos.setCode(req.code());
        pos.setName(req.name());
        return toPosResponse(posRepository.save(pos));
    }

    @Transactional
    public void deactivatePointOfSale(String companyId, String branchId, String posId) {
        requireBranch(companyId, branchId);
        PointOfSale pos = posRepository.findById(posId)
                .filter(x -> x.getBranchId().equals(branchId))
                .orElseThrow(() -> new ResourceNotFoundException("POS not found: " + posId));
        pos.setActive(false);
        posRepository.save(pos);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void requireCompany(String companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found: " + companyId);
        }
    }

    private void requireBranch(String companyId, String branchId) {
        branchRepository.findById(branchId)
                .filter(b -> b.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }

    private void applyBranch(Branch b, BranchDto.Request req) {
        b.setCode(req.code());
        b.setName(req.name());
        b.setAddressLine1(req.addressLine1());
        b.setDepartmentCode(req.departmentCode());
        b.setMunicipalityCode(req.municipalityCode());
        b.setPhone(req.phone());
    }

    private BranchDto.Response toBranchResponse(Branch b) {
        return new BranchDto.Response(
                b.getId(), b.getCompanyId(), b.getCode(), b.getName(),
                b.getAddressLine1(), b.getDepartmentCode(), b.getMunicipalityCode(),
                b.getPhone(), b.isActive()
        );
    }

    private PointOfSaleDto.Response toPosResponse(PointOfSale pos) {
        return new PointOfSaleDto.Response(
                pos.getId(), pos.getBranchId(), pos.getCode(), pos.getName(), pos.isActive()
        );
    }
}
