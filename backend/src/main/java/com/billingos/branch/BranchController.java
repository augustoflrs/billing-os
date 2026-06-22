package com.billingos.branch;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    // ─── Branches ─────────────────────────────────────────────────────────────

    @GetMapping("/branches")
    public List<BranchDto.Response> listBranches(@PathVariable String companyId) {
        return branchService.listBranches(companyId);
    }

    @PostMapping("/branches")
    public ResponseEntity<BranchDto.Response> createBranch(
            @PathVariable String companyId,
            @Valid @RequestBody BranchDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.createBranch(companyId, request));
    }

    @PutMapping("/branches/{branchId}")
    public BranchDto.Response updateBranch(
            @PathVariable String companyId,
            @PathVariable String branchId,
            @Valid @RequestBody BranchDto.Request request) {
        return branchService.updateBranch(companyId, branchId, request);
    }

    @DeleteMapping("/branches/{branchId}")
    public ResponseEntity<Void> deactivateBranch(
            @PathVariable String companyId,
            @PathVariable String branchId) {
        branchService.deactivateBranch(companyId, branchId);
        return ResponseEntity.noContent().build();
    }

    // ─── Points of Sale ───────────────────────────────────────────────────────

    @GetMapping("/branches/{branchId}/pos")
    public List<PointOfSaleDto.Response> listPos(
            @PathVariable String companyId,
            @PathVariable String branchId) {
        return branchService.listPointsOfSale(companyId, branchId);
    }

    @PostMapping("/branches/{branchId}/pos")
    public ResponseEntity<PointOfSaleDto.Response> createPos(
            @PathVariable String companyId,
            @PathVariable String branchId,
            @Valid @RequestBody PointOfSaleDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchService.createPointOfSale(companyId, branchId, request));
    }

    @PutMapping("/branches/{branchId}/pos/{posId}")
    public PointOfSaleDto.Response updatePos(
            @PathVariable String companyId,
            @PathVariable String branchId,
            @PathVariable String posId,
            @Valid @RequestBody PointOfSaleDto.Request request) {
        return branchService.updatePointOfSale(companyId, branchId, posId, request);
    }

    @DeleteMapping("/branches/{branchId}/pos/{posId}")
    public ResponseEntity<Void> deactivatePos(
            @PathVariable String companyId,
            @PathVariable String branchId,
            @PathVariable String posId) {
        branchService.deactivatePointOfSale(companyId, branchId, posId);
        return ResponseEntity.noContent().build();
    }
}
