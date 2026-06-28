package com.billingos.invoice;

import com.billingos.branch.PointOfSale;
import com.billingos.branch.PointOfSaleRepository;
import com.billingos.catalog.TaxDefinition;
import com.billingos.catalog.TaxDefinitionRepository;
import com.billingos.common.exception.ResourceNotFoundException;
import com.billingos.common.outbox.OutboxEvent;
import com.billingos.common.outbox.OutboxEventRepository;
import com.billingos.common.status.EntityStatusHistoryRepository;
import com.billingos.common.status.StatusMachineService;
import com.billingos.customer.Customer;
import com.billingos.customer.CustomerRepository;
import com.billingos.config.AppProperties;
import com.billingos.dte.DteInvalidationService;
import com.billingos.invoice.InvoiceDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    static final String DRAFT_STATUS_ID     = "inv_draft";
    static final String ISSUED_STATUS_ID    = "inv_issued";
    static final String CANCELLED_STATUS_ID = "inv_cancelled";
    private static final String ENTITY_TYPE = "INVOICE";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceSequenceRepository sequenceRepository;
    private final CustomerRepository customerRepository;
    private final PointOfSaleRepository pointOfSaleRepository;
    private final TaxDefinitionRepository taxDefinitionRepository;
    private final StatusMachineService statusMachine;
    private final OutboxEventRepository outboxRepository;
    private final EntityStatusHistoryRepository statusHistoryRepository;
    private final DteInvalidationService dteInvalidationService;
    private final AppProperties appProperties;

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        PointOfSale pos = pointOfSaleRepository.findById(req.pointOfSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Point of sale not found"));

        Map<String, BigDecimal> taxRates = taxDefinitionRepository.findByActiveTrueOrderByName()
                .stream()
                .collect(Collectors.toMap(TaxDefinition::getCode,
                        t -> t.getRate() != null ? t.getRate() : BigDecimal.ZERO));

        Invoice invoice = new Invoice();
        invoice.setCustomerId(customer.getId());
        invoice.setPointOfSaleId(pos.getId());
        invoice.setDocumentTypeCode(req.documentTypeCode());
        invoice.setInvoiceDate(req.invoiceDate() != null ? req.invoiceDate() : Instant.now());
        invoice.setCurrentStatusId(DRAFT_STATUS_ID);

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (LineRequest lr : req.lines()) {
            if (lr.itemName() == null || lr.itemName().isBlank()) {
                throw new IllegalArgumentException("itemName is required on each line");
            }
            BigDecimal qty      = lr.quantity();
            BigDecimal price    = lr.unitPrice();
            BigDecimal discount = lr.discountAmount() != null ? lr.discountAmount() : BigDecimal.ZERO;
            BigDecimal subtotal = qty.multiply(price).subtract(discount).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineTax  = BigDecimal.ZERO;

            InvoiceLine line = new InvoiceLine();
            line.setInvoice(invoice);
            line.setBillableItemId(lr.billableItemId());
            line.setItemName(lr.itemName());
            line.setItemDescription(lr.itemDescription());
            line.setQuantity(qty);
            line.setUnitPrice(price);
            line.setSubtotalAmount(subtotal);
            line.setDiscountAmount(discount);

            if (lr.taxCode() != null && !lr.taxCode().isBlank()) {
                BigDecimal rate   = taxRates.getOrDefault(lr.taxCode(), BigDecimal.ZERO);
                BigDecimal taxAmt = subtotal.multiply(rate).setScale(4, RoundingMode.HALF_UP);

                InvoiceLineTax tax = new InvoiceLineTax();
                tax.setInvoiceLine(line);
                tax.setTaxCode(lr.taxCode());
                tax.setRate(rate);
                tax.setTaxableAmount(subtotal);
                tax.setTaxAmount(taxAmt);
                line.getTaxes().add(tax);
                lineTax = taxAmt;
            }

            line.setTaxAmount(lineTax);
            line.setTotalAmount(subtotal.add(lineTax));
            invoice.getLines().add(line);

            totalSubtotal = totalSubtotal.add(subtotal);
            totalDiscount = totalDiscount.add(discount);
            totalTax      = totalTax.add(lineTax);
        }

        BigDecimal total = totalSubtotal.add(totalTax);
        invoice.setSubtotalAmount(totalSubtotal);
        invoice.setDiscountAmount(totalDiscount);
        invoice.setTaxAmount(totalTax);
        invoice.setTotalAmount(total);
        invoice.setBalanceAmount(total);

        invoiceRepository.save(invoice);
        return toResponse(invoice, customer.getLegalName());
    }

    @Transactional
    public InvoiceResponse confirm(String id) {
        Invoice invoice = getOrThrow(id);

        // Validate + write history/audit via status machine
        statusMachine.transition(ENTITY_TYPE, id,
                invoice.getCurrentStatusId(), ISSUED_STATUS_ID, "Invoice confirmed by user");

        String number = nextInvoiceNumber(invoice.getPointOfSaleId(), invoice.getDocumentTypeCode());
        invoice.setInvoiceNumber(number);
        invoice.setCurrentStatusId(ISSUED_STATUS_ID);
        int dueDays = appProperties.getInvoice().getDefaultDueDays();
        invoice.setDueDate(invoice.getInvoiceDate().plus(dueDays, java.time.temporal.ChronoUnit.DAYS));
        invoiceRepository.save(invoice);

        // Write outbox event for DTE pipeline (T-12–T-14)
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("INVOICE");
        event.setAggregateId(id);
        event.setEventType("INVOICE_CONFIRMED");
        event.setPayload(Map.of(
                "invoiceId", id,
                "documentTypeCode", invoice.getDocumentTypeCode()
        ));
        outboxRepository.save(event);

        String customerName = customerRepository.findById(invoice.getCustomerId())
                .map(Customer::getLegalName).orElse("");
        return toResponse(invoice, customerName);
    }

    @Transactional
    public InvoiceResponse cancel(String id, String reason) {
        Invoice invoice = getOrThrow(id);

        // Attempt DTE invalidation with MH before cancelling the invoice.
        // Non-fatal: if MH rejects or is unreachable, we still cancel locally.
        try {
            dteInvalidationService.invalidate(id, reason);
        } catch (Exception e) {
            // Log but don't block the cancellation
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .error("DTE invalidation failed for invoice {}: {}", id, e.getMessage(), e);
        }

        statusMachine.transition(ENTITY_TYPE, id,
                invoice.getCurrentStatusId(), CANCELLED_STATUS_ID,
                reason != null ? reason : "Invoice cancelled");

        invoice.setCurrentStatusId(CANCELLED_STATUS_ID);
        invoiceRepository.save(invoice);

        String customerName = customerRepository.findById(invoice.getCustomerId())
                .map(Customer::getLegalName).orElse("");
        return toResponse(invoice, customerName);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(String id) {
        Invoice invoice = getOrThrow(id);
        String customerName = customerRepository.findById(invoice.getCustomerId())
                .map(Customer::getLegalName).orElse("");
        return toResponse(invoice, customerName);
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse list(String search, String statusCode, String customerId,
                                     LocalDate from, LocalDate to, int page, int size) {
        String statusId = statusCode != null && !statusCode.isBlank()
                ? statusCodeToId(statusCode) : null;
        Instant fromInstant = from != null
                ? from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                : Instant.EPOCH;
        Instant toInstant = to != null
                ? to.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
                : Instant.now().plusSeconds(60L * 60 * 24 * 365 * 100);

        Page<Invoice> pg = invoiceRepository.search(
                search, statusId, customerId, fromInstant, toInstant,
                PageRequest.of(page, size));

        List<String> customerIds = pg.getContent().stream().map(Invoice::getCustomerId).distinct().toList();
        Map<String, String> customerNames = customerRepository.findAllById(customerIds)
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getLegalName));

        List<InvoiceSummary> summaries = pg.getContent().stream()
                .map(i -> toSummary(i, customerNames.getOrDefault(i.getCustomerId(), "")))
                .toList();

        return new InvoicePageResponse(summaries, page, size, pg.getTotalElements(), pg.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto.StatusHistoryEntry> statusHistory(String id) {
        getOrThrow(id); // ensure exists
        return statusHistoryRepository.findByEntityTypeAndEntityIdOrderByChangedAtAsc(ENTITY_TYPE, id)
                .stream()
                .map(h -> new InvoiceDto.StatusHistoryEntry(
                        statusCode(h.getOldStatusId()),
                        statusCode(h.getNewStatusId()),
                        h.getChangedBy(),
                        h.getChangedAt(),
                        h.getReason()))
                .toList();
    }

    // ── helpers ──────────────────────────────────────────────────

    private Invoice getOrThrow(String id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private String nextInvoiceNumber(String posId, String docType) {
        InvoiceSequence seq = sequenceRepository.findByPosAndDocTypeLocked(posId, docType)
                .orElseGet(() -> {
                    InvoiceSequence s = new InvoiceSequence();
                    s.setPointOfSaleId(posId);
                    s.setDocumentTypeCode(docType);
                    return s;
                });
        seq.setCurrentValue(seq.getCurrentValue() + 1);
        sequenceRepository.save(seq);
        return String.format("%s-%08d", docType, seq.getCurrentValue());
    }

    private InvoiceResponse toResponse(Invoice i, String customerName) {
        List<LineResponse> lines = i.getLines().stream().map(l -> new LineResponse(
                l.getId(),
                l.getBillableItemId(),
                l.getItemName(),
                l.getItemDescription(),
                l.getQuantity(),
                l.getUnitPrice(),
                l.getSubtotalAmount(),
                l.getDiscountAmount(),
                l.getTaxAmount(),
                l.getTotalAmount(),
                l.getTaxes().stream().map(t -> new TaxResponse(
                        t.getTaxCode(), t.getRate(), t.getTaxableAmount(), t.getTaxAmount()
                )).toList()
        )).toList();

        return new InvoiceResponse(
                i.getId(), i.getInvoiceNumber(),
                i.getCustomerId(), customerName,
                i.getPointOfSaleId(), i.getDocumentTypeCode(),
                i.getInvoiceDate(), i.getDueDate(),
                i.getSubtotalAmount(), i.getDiscountAmount(),
                i.getTaxAmount(), i.getTotalAmount(),
                i.getPaidAmount(), i.getBalanceAmount(),
                statusCode(i.getCurrentStatusId()), statusName(i.getCurrentStatusId()),
                lines
        );
    }

    private InvoiceSummary toSummary(Invoice i, String customerName) {
        return new InvoiceSummary(
                i.getId(), i.getInvoiceNumber(),
                i.getCustomerId(), customerName,
                i.getDocumentTypeCode(), i.getInvoiceDate(), i.getDueDate(),
                i.getTotalAmount(), i.getBalanceAmount(),
                statusCode(i.getCurrentStatusId()), statusName(i.getCurrentStatusId())
        );
    }

    private String statusCode(String statusId) {
        if (statusId == null) return null;
        return switch (statusId) {
            case "inv_draft"     -> "DRAFT";
            case "inv_issued"    -> "ISSUED";
            case "inv_partial"   -> "PARTIAL";
            case "inv_paid"      -> "PAID";
            case "inv_cancelled" -> "CANCELLED";
            case "inv_overdue"   -> "OVERDUE";
            default              -> statusId;
        };
    }

    private String statusName(String statusId) {
        if (statusId == null) return null;
        return switch (statusId) {
            case "inv_draft"     -> "Borrador";
            case "inv_issued"    -> "Emitida";
            case "inv_partial"   -> "Pago Parcial";
            case "inv_paid"      -> "Pagada";
            case "inv_cancelled" -> "Anulada";
            case "inv_overdue"   -> "Vencida";
            default              -> statusId;
        };
    }

    private String statusCodeToId(String code) {
        return switch (code.toUpperCase()) {
            case "DRAFT"     -> "inv_draft";
            case "ISSUED"    -> "inv_issued";
            case "PARTIAL"   -> "inv_partial";
            case "PAID"      -> "inv_paid";
            case "CANCELLED" -> "inv_cancelled";
            case "OVERDUE"   -> "inv_overdue";
            default          -> null;
        };
    }
}
