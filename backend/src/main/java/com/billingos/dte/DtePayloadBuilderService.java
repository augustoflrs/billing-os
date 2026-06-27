package com.billingos.dte;

import com.billingos.branch.Branch;
import com.billingos.branch.BranchRepository;
import com.billingos.branch.PointOfSale;
import com.billingos.branch.PointOfSaleRepository;
import com.billingos.company.Company;
import com.billingos.company.CompanyRepository;
import com.billingos.customer.Customer;
import com.billingos.customer.CustomerRepository;
import com.billingos.customer.CustomerTaxProfile;
import com.billingos.customer.CustomerTaxProfileRepository;
import com.billingos.invoice.Invoice;
import com.billingos.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DtePayloadBuilderService {

    private final InvoiceRepository          invoiceRepository;
    private final CompanyRepository          companyRepository;
    private final BranchRepository           branchRepository;
    private final PointOfSaleRepository      posRepository;
    private final CustomerRepository         customerRepository;
    private final CustomerTaxProfileRepository taxProfileRepository;
    private final InvoiceDteRepository       dteRepository;
    private final List<DteDocumentStrategy>  strategies;

    private Map<String, DteDocumentStrategy> strategyMap;

    @jakarta.annotation.PostConstruct
    void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(DteDocumentStrategy::documentTypeCode, Function.identity()));
    }

    /**
     * Builds the MH DTE JSON payload for the given invoice, persists an InvoiceDte
     * record in dte_draft status, and returns it.
     *
     * Idempotent: if a DTE record already exists for this invoice, returns it as-is.
     */
    @Transactional
    public InvoiceDte buildAndPersist(String invoiceId) {
        if (dteRepository.existsByInvoiceId(invoiceId)) {
            log.info("DTE already exists for invoice {}, skipping", invoiceId);
            return dteRepository.findByInvoiceId(invoiceId).orElseThrow();
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        DteDocumentStrategy strategy = strategyMap.get(invoice.getDocumentTypeCode());
        if (strategy == null) {
            throw new IllegalStateException(
                "No DTE strategy for document type: " + invoice.getDocumentTypeCode());
        }

        PointOfSale pos    = posRepository.findById(invoice.getPointOfSaleId())
                .orElseThrow(() -> new IllegalStateException("POS not found: " + invoice.getPointOfSaleId()));
        Branch branch      = branchRepository.findById(pos.getBranchId())
                .orElseThrow(() -> new IllegalStateException("Branch not found: " + pos.getBranchId()));
        Company company    = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No company configured"));
        Customer customer  = customerRepository.findById(invoice.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Customer not found: " + invoice.getCustomerId()));
        CustomerTaxProfile taxProfile = taxProfileRepository
                .findByCustomer_Id(invoice.getCustomerId())
                .orElse(null);

        String generationCode = UUID.randomUUID().toString().toUpperCase();
        String controlNumber  = buildControlNumber(
                invoice.getDocumentTypeCode(), branch.getCode(), pos.getCode(), invoice.getInvoiceNumber());

        DteContext ctx = new DteContext(invoice, pos, branch, company, customer, taxProfile);
        Map<String, Object> payload = strategy.buildPayload(ctx, generationCode, controlNumber);

        InvoiceDte dte = new InvoiceDte();
        dte.setInvoiceId(invoiceId);
        dte.setGenerationCode(generationCode);
        dte.setControlNumber(controlNumber);
        dte.setRequestPayload(payload);
        dte.setCurrentStatusId("dte_draft");

        InvoiceDte saved = dteRepository.save(dte);
        log.info("Built DTE {} for invoice {} — controlNumber={}", saved.getId(), invoiceId, controlNumber);
        return saved;
    }

    /**
     * Builds control number per MH format:
     * DTE-{tipoDte}-{branchCode4}{posCode4}-{seq15digits}
     *
     * The sequential part is derived from the invoice number (e.g. "01-00000001" → 1).
     */
    private static String buildControlNumber(String docType, String branchCode,
                                             String posCode, String invoiceNumber) {
        long seq = 1L;
        if (invoiceNumber != null && invoiceNumber.contains("-")) {
            try {
                seq = Long.parseLong(invoiceNumber.substring(invoiceNumber.lastIndexOf('-') + 1));
            } catch (NumberFormatException ignored) {}
        }
        String estab = BaseDtePayloadBuilder.pad4(branchCode);
        String pv    = BaseDtePayloadBuilder.pad4(posCode);
        return String.format("DTE-%s-%s%s-%015d", docType, estab, pv, seq);
    }
}
