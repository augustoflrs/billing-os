package com.billingos.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class DashboardDto {

    public record InvoiceMetrics(
            long draft,
            long issued,
            long partial,
            long overdue,
            long paid,
            long cancelled,
            BigDecimal outstandingBalance,   // sum of balance_amount for issued/partial/overdue
            BigDecimal revenueThisMonth,     // sum of total_amount confirmed this month
            BigDecimal collectedThisMonth    // sum of payments recorded this month
    ) {}

    public record DtePendingAlert(
            String invoiceId,
            String invoiceNumber,
            String customerName,
            String dteStatusCode,
            String dteStatusName,
            int attemptCount,
            Instant nextAttemptAt,
            Instant submittedAt
    ) {}

    public record RecentInvoice(
            String id,
            String invoiceNumber,
            String customerName,
            BigDecimal totalAmount,
            BigDecimal balanceAmount,
            String statusCode,
            String statusName,
            Instant invoiceDate
    ) {}

    public record DashboardMetrics(
            InvoiceMetrics invoices,
            List<DtePendingAlert> dtePendingAlerts,
            List<RecentInvoice> recentInvoices
    ) {}
}
