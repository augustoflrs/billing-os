import { apiClient } from "@/lib/api-client";

export interface InvoiceMetrics {
  draft: number;
  issued: number;
  partial: number;
  overdue: number;
  paid: number;
  cancelled: number;
  outstandingBalance: number;
  revenueThisMonth: number;
  collectedThisMonth: number;
}

export interface DtePendingAlert {
  invoiceId: string;
  invoiceNumber?: string;
  customerName: string;
  dteStatusCode: string;
  dteStatusName: string;
  attemptCount: number;
  nextAttemptAt?: string;
  submittedAt?: string;
}

export interface RecentInvoice {
  id: string;
  invoiceNumber?: string;
  customerName: string;
  totalAmount: number;
  balanceAmount: number;
  statusCode: string;
  statusName: string;
  invoiceDate: string;
}

export interface DashboardMetrics {
  invoices: InvoiceMetrics;
  dtePendingAlerts: DtePendingAlert[];
  recentInvoices: RecentInvoice[];
}

export const dashboardApi = {
  metrics: () =>
    apiClient.get<DashboardMetrics>("/dashboard/metrics").then((r) => r.data),
};
