package com.billingos.customer;

import com.billingos.common.UlidGenerator;
import com.billingos.common.exception.ConflictException;
import com.billingos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public CustomerDto.PageResponse list(String search, int page, int size) {
        Page<Customer> result = customerRepository.search(
                search, PageRequest.of(page, size));
        return new CustomerDto.PageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public CustomerDto.Response get(String id) {
        return toResponse(find(id));
    }

    @Transactional
    public CustomerDto.Response create(CustomerDto.Request req) {
        String number = nextCustomerNumber();
        if (customerRepository.existsByCustomerNumber(number)) {
            throw new ConflictException("Customer number conflict, retry");
        }

        Customer c = new Customer();
        c.setId(UlidGenerator.generate());
        c.setCustomerNumber(number);
        c.setCreatedBy(currentUser());
        applyFields(c, req);

        if (req.address() != null) {
            CustomerAddress addr = buildAddress(c, req.address());
            c.setAddress(addr);
        }
        if (req.taxProfile() != null) {
            CustomerTaxProfile tp = buildTaxProfile(c, req.taxProfile());
            c.setTaxProfile(tp);
        }

        return toResponse(customerRepository.save(c));
    }

    @Transactional
    public CustomerDto.Response update(String id, CustomerDto.Request req, Long expectedVersion) {
        Customer c = find(id);
        if (!c.getVersion().equals(expectedVersion)) {
            throw new ConflictException("Stale version: expected " + expectedVersion
                    + " but found " + c.getVersion() + ". Reload and retry.");
        }

        applyFields(c, req);
        c.setUpdatedAt(OffsetDateTime.now());
        c.setUpdatedBy(currentUser());

        if (req.address() != null) {
            if (c.getAddress() != null) {
                applyAddress(c.getAddress(), req.address());
            } else {
                c.setAddress(buildAddress(c, req.address()));
            }
        } else {
            c.setAddress(null);
        }

        if (req.taxProfile() != null) {
            if (c.getTaxProfile() != null) {
                applyTaxProfile(c.getTaxProfile(), req.taxProfile());
            } else {
                c.setTaxProfile(buildTaxProfile(c, req.taxProfile()));
            }
        } else {
            c.setTaxProfile(null);
        }

        return toResponse(customerRepository.save(c));
    }

    @Transactional
    public void deactivate(String id) {
        Customer c = find(id);
        c.setStatus("INACTIVE");
        c.setUpdatedAt(OffsetDateTime.now());
        c.setUpdatedBy(currentUser());
        customerRepository.save(c);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Customer find(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private void applyFields(Customer c, CustomerDto.Request req) {
        c.setLegalName(req.legalName());
        c.setTradeName(req.tradeName());
        c.setEmail(req.email());
        c.setPhone(req.phone());
    }

    private CustomerAddress buildAddress(Customer c, CustomerDto.AddressInput in) {
        CustomerAddress addr = new CustomerAddress();
        addr.setId(UlidGenerator.generate());
        addr.setCustomer(c);
        applyAddress(addr, in);
        return addr;
    }

    private void applyAddress(CustomerAddress addr, CustomerDto.AddressInput in) {
        addr.setAddressLine1(in.addressLine1());
        addr.setDepartmentCode(in.departmentCode());
        addr.setMunicipalityCode(in.municipalityCode());
    }

    private CustomerTaxProfile buildTaxProfile(Customer c, CustomerDto.TaxProfileInput in) {
        CustomerTaxProfile tp = new CustomerTaxProfile();
        tp.setId(UlidGenerator.generate());
        tp.setCustomer(c);
        applyTaxProfile(tp, in);
        return tp;
    }

    private void applyTaxProfile(CustomerTaxProfile tp, CustomerDto.TaxProfileInput in) {
        tp.setDocumentType(in.documentType());
        tp.setDocumentNumber(in.documentNumber());
        tp.setNit(in.nit());
        tp.setNrc(in.nrc());
        tp.setEconomicActivityCode(in.economicActivityCode());
    }

    private String nextCustomerNumber() {
        long count = customerRepository.count();
        return String.format("CLI-%06d", count + 1);
    }

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    CustomerDto.Response toResponse(Customer c) {
        CustomerDto.AddressResponse addr = c.getAddress() == null ? null :
                new CustomerDto.AddressResponse(
                        c.getAddress().getId(),
                        c.getAddress().getAddressLine1(),
                        c.getAddress().getDepartmentCode(),
                        c.getAddress().getMunicipalityCode(),
                        c.getAddress().isDefault()
                );

        CustomerDto.TaxProfileResponse tp = c.getTaxProfile() == null ? null :
                new CustomerDto.TaxProfileResponse(
                        c.getTaxProfile().getId(),
                        c.getTaxProfile().getDocumentType(),
                        c.getTaxProfile().getDocumentNumber(),
                        c.getTaxProfile().getNit(),
                        c.getTaxProfile().getNrc(),
                        c.getTaxProfile().getEconomicActivityCode()
                );

        return new CustomerDto.Response(
                c.getId(), c.getCustomerNumber(), c.getLegalName(), c.getTradeName(),
                c.getEmail(), c.getPhone(), c.getStatus(),
                c.getCreatedAt(), c.getUpdatedAt(), c.getVersion(),
                addr, tp
        );
    }
}
