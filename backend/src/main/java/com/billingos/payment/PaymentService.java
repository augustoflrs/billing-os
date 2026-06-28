package com.billingos.payment;

import com.billingos.common.status.StatusMachineService;
import com.billingos.invoice.Invoice;
import com.billingos.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String ENTITY_INVOICE = "INVOICE";
    private static final Set<String> PAYABLE_STATUSES = Set.of("inv_issued", "inv_partial", "inv_overdue");

    private final PaymentRepository        paymentRepository;
    private final InvoiceRepository        invoiceRepository;
    private final StatusMachineService     statusMachine;

    @Transactional
    public PaymentDto.PaymentResponse record(PaymentDto.CreatePaymentRequest req) {
        Instant paymentDate = req.paymentDate() != null ? req.paymentDate() : Instant.now();

        // Load and validate all invoices upfront
        List<InvoiceAllocationPair> pairs = req.allocations().stream().map(a -> {
            Invoice inv = invoiceRepository.findById(a.invoiceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Invoice not found: " + a.invoiceId()));

            if (!PAYABLE_STATUSES.contains(inv.getCurrentStatusId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Invoice " + inv.getInvoiceNumber() + " cannot receive payment in status "
                                + inv.getCurrentStatusId());
            }

            BigDecimal balance = inv.getBalanceAmount();
            if (a.amount().compareTo(balance) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Allocated amount " + a.amount() + " exceeds balance " + balance
                                + " on invoice " + inv.getInvoiceNumber());
            }

            return new InvoiceAllocationPair(inv, a.amount());
        }).toList();

        // Build payment
        Payment payment = new Payment();
        payment.setPaymentDate(paymentDate);
        payment.setPaymentMethodCode(req.paymentMethodCode());
        payment.setReferenceNumber(req.referenceNumber());
        payment.setCurrentStatusId("pay_confirmed");

        BigDecimal total = req.allocations().stream()
                .map(PaymentDto.AllocationRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        payment.setAmount(total);

        // Build allocations and update invoice balances
        for (InvoiceAllocationPair pair : pairs) {
            PaymentAllocation alloc = new PaymentAllocation();
            alloc.setPayment(payment);
            alloc.setInvoice(pair.invoice());
            alloc.setAllocatedAmount(pair.amount());
            payment.getAllocations().add(alloc);

            Invoice inv = pair.invoice();
            BigDecimal newPaid    = inv.getPaidAmount().add(pair.amount());
            BigDecimal newBalance = inv.getBalanceAmount().subtract(pair.amount());
            inv.setPaidAmount(newPaid);
            inv.setBalanceAmount(newBalance);

            // Transition invoice status
            String from = inv.getCurrentStatusId();
            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                statusMachine.transition(ENTITY_INVOICE, inv.getId(), from, "inv_paid",
                        "Fully paid via payment");
                inv.setCurrentStatusId("inv_paid");
            } else {
                if (!"inv_partial".equals(from)) {
                    statusMachine.transition(ENTITY_INVOICE, inv.getId(), from, "inv_partial",
                            "Partial payment received");
                    inv.setCurrentStatusId("inv_partial");
                }
            }
            invoiceRepository.save(inv);
        }

        payment = paymentRepository.save(payment);
        log.info("Payment {} recorded: {} {} for {} invoice(s)",
                payment.getId(), total, req.paymentMethodCode(), pairs.size());

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto.PaymentResponse> listByInvoice(String invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private PaymentDto.PaymentResponse toResponse(Payment p) {
        List<PaymentDto.AllocationResponse> allocs = p.getAllocations().stream()
                .map(a -> new PaymentDto.AllocationResponse(
                        a.getId(),
                        a.getInvoice().getId(),
                        a.getInvoice().getInvoiceNumber(),
                        a.getAllocatedAmount()))
                .toList();

        return new PaymentDto.PaymentResponse(
                p.getId(),
                p.getPaymentDate(),
                p.getAmount(),
                p.getPaymentMethodCode(),
                p.getReferenceNumber(),
                p.getCurrentStatusId().replace("pay_", "").toUpperCase(),
                allocs);
    }

    private record InvoiceAllocationPair(Invoice invoice, BigDecimal amount) {}
}
