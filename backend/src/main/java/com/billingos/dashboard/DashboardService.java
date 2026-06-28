package com.billingos.dashboard;

import com.billingos.customer.CustomerRepository;
import com.billingos.dte.DteTransmissionQueue;
import com.billingos.dte.DteTransmissionQueueRepository;
import com.billingos.dte.InvoiceDte;
import com.billingos.dte.InvoiceDteRepository;
import com.billingos.invoice.Invoice;
import com.billingos.invoice.InvoiceRepository;
import com.billingos.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId SV_ZONE = ZoneId.of("America/El_Salvador");

    private final InvoiceRepository          invoiceRepository;
    private final InvoiceDteRepository       dteRepository;
    private final DteTransmissionQueueRepository queueRepository;
    private final CustomerRepository         customerRepository;
    private final PaymentRepository          paymentRepository;

    @Transactional(readOnly = true)
    public DashboardDto.DashboardMetrics metrics() {
        return new DashboardDto.DashboardMetrics(
                invoiceMetrics(),
                dtePendingAlerts(),
                recentInvoices()
        );
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private DashboardDto.InvoiceMetrics invoiceMetrics() {
        List<Invoice> all = invoiceRepository.findAll();

        long draft     = count(all, "inv_draft");
        long issued    = count(all, "inv_issued");
        long partial   = count(all, "inv_partial");
        long overdue   = count(all, "inv_overdue");
        long paid      = count(all, "inv_paid");
        long cancelled = count(all, "inv_cancelled");

        BigDecimal outstanding = all.stream()
                .filter(i -> List.of("inv_issued", "inv_partial", "inv_overdue")
                        .contains(i.getCurrentStatusId()))
                .map(Invoice::getBalanceAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ZonedDateTime startOfMonth = ZonedDateTime.now(SV_ZONE).withDayOfMonth(1)
                .toLocalDate().atStartOfDay(SV_ZONE);
        Instant monthStart = startOfMonth.toInstant();

        BigDecimal revenueThisMonth = all.stream()
                .filter(i -> !List.of("inv_draft", "inv_cancelled").contains(i.getCurrentStatusId()))
                .filter(i -> i.getInvoiceDate() != null && !i.getInvoiceDate().isBefore(monthStart))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal collectedThisMonth = paymentRepository.sumAmountSince(monthStart)
                .orElse(BigDecimal.ZERO);

        return new DashboardDto.InvoiceMetrics(
                draft, issued, partial, overdue, paid, cancelled,
                outstanding, revenueThisMonth, collectedThisMonth);
    }

    private List<DashboardDto.DtePendingAlert> dtePendingAlerts() {
        List<String> alertStatuses = List.of(
                "dte_queued", "dte_submitted", "dte_rejected", "dte_contingency", "dte_blocked");

        List<InvoiceDte> pendingDtes = dteRepository.findAll().stream()
                .filter(d -> alertStatuses.contains(d.getCurrentStatusId()))
                .toList();

        if (pendingDtes.isEmpty()) return List.of();

        // Load invoices + customers in bulk
        List<String> invoiceIds = pendingDtes.stream().map(InvoiceDte::getInvoiceId).toList();
        Map<String, Invoice> invoiceMap = invoiceRepository.findAllById(invoiceIds)
                .stream().collect(Collectors.toMap(Invoice::getId, i -> i));

        List<String> customerIds = invoiceMap.values().stream()
                .map(Invoice::getCustomerId).distinct().toList();
        Map<String, String> customerNames = customerRepository.findAllById(customerIds)
                .stream().collect(Collectors.toMap(
                        com.billingos.customer.Customer::getId,
                        com.billingos.customer.Customer::getLegalName));

        Map<String, DteTransmissionQueue> queueMap = queueRepository
                .findAll().stream()
                .collect(Collectors.toMap(DteTransmissionQueue::getInvoiceDteId, q -> q,
                        (a, b) -> a));

        return pendingDtes.stream().map(dte -> {
            Invoice inv = invoiceMap.get(dte.getInvoiceId());
            String custName = inv != null ? customerNames.getOrDefault(inv.getCustomerId(), "") : "";
            DteTransmissionQueue q = queueMap.get(dte.getId());
            return new DashboardDto.DtePendingAlert(
                    dte.getInvoiceId(),
                    inv != null ? inv.getInvoiceNumber() : null,
                    custName,
                    dteStatusCode(dte.getCurrentStatusId()),
                    dteStatusName(dte.getCurrentStatusId()),
                    q != null ? q.getAttemptCount() : 0,
                    q != null ? q.getNextAttemptAt() : null,
                    dte.getSubmittedAt()
            );
        }).toList();
    }

    private List<DashboardDto.RecentInvoice> recentInvoices() {
        List<Invoice> recent = invoiceRepository
                .findTop10ByCurrentStatusIdNotOrderByInvoiceDateDesc("inv_draft");

        List<String> customerIds = recent.stream().map(Invoice::getCustomerId).distinct().toList();
        Map<String, String> names = customerRepository.findAllById(customerIds)
                .stream().collect(Collectors.toMap(
                        com.billingos.customer.Customer::getId,
                        com.billingos.customer.Customer::getLegalName));

        return recent.stream().map(i -> new DashboardDto.RecentInvoice(
                i.getId(),
                i.getInvoiceNumber(),
                names.getOrDefault(i.getCustomerId(), ""),
                i.getTotalAmount(),
                i.getBalanceAmount(),
                invoiceStatusCode(i.getCurrentStatusId()),
                invoiceStatusName(i.getCurrentStatusId()),
                i.getInvoiceDate()
        )).toList();
    }

    private static long count(List<Invoice> all, String statusId) {
        return all.stream().filter(i -> statusId.equals(i.getCurrentStatusId())).count();
    }

    private static String dteStatusCode(String id) {
        return switch (id) {
            case "dte_queued"      -> "QUEUED";
            case "dte_submitted"   -> "SUBMITTED";
            case "dte_rejected"    -> "REJECTED";
            case "dte_contingency" -> "CONTINGENCY";
            case "dte_blocked"     -> "BLOCKED";
            default -> id;
        };
    }

    private static String dteStatusName(String id) {
        return switch (id) {
            case "dte_queued"      -> "En Cola";
            case "dte_submitted"   -> "Enviado";
            case "dte_rejected"    -> "Rechazado";
            case "dte_contingency" -> "Contingencia";
            case "dte_blocked"     -> "Bloqueado";
            default -> id;
        };
    }

    private static String invoiceStatusCode(String id) {
        return switch (id) {
            case "inv_issued"    -> "ISSUED";
            case "inv_partial"   -> "PARTIAL";
            case "inv_overdue"   -> "OVERDUE";
            case "inv_paid"      -> "PAID";
            case "inv_cancelled" -> "CANCELLED";
            default -> id.toUpperCase();
        };
    }

    private static String invoiceStatusName(String id) {
        return switch (id) {
            case "inv_issued"    -> "Emitida";
            case "inv_partial"   -> "Pago Parcial";
            case "inv_overdue"   -> "Vencida";
            case "inv_paid"      -> "Pagada";
            case "inv_cancelled" -> "Anulada";
            default -> id;
        };
    }
}
