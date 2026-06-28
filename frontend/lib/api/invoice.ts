import { apiClient } from "@/lib/api-client";

export interface InvoiceLineTaxResponse {
  taxCode: string;
  rate: number;
  taxableAmount: number;
  taxAmount: number;
}

export interface InvoiceLineResponse {
  id: string;
  billableItemId?: string;
  itemName: string;
  itemDescription?: string;
  quantity: number;
  unitPrice: number;
  subtotalAmount: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  taxes: InvoiceLineTaxResponse[];
}

export interface InvoiceResponse {
  id: string;
  invoiceNumber?: string;
  customerId: string;
  customerName: string;
  pointOfSaleId: string;
  documentTypeCode: string;
  invoiceDate: string;
  subtotalAmount: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceAmount: number;
  statusCode: string;
  statusName: string;
  lines: InvoiceLineResponse[];
}

export interface InvoiceSummary {
  id: string;
  invoiceNumber?: string;
  customerId: string;
  customerName: string;
  documentTypeCode: string;
  invoiceDate: string;
  totalAmount: number;
  balanceAmount: number;
  statusCode: string;
  statusName: string;
}

export interface InvoicePage {
  content: InvoiceSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LineRequest {
  billableItemId?: string;
  itemName: string;
  itemDescription?: string;
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  taxCode?: string;
}

export interface CreateInvoiceRequest {
  customerId: string;
  pointOfSaleId: string;
  documentTypeCode: string;
  invoiceDate?: string;
  lines: LineRequest[];
}

export interface StatusHistoryEntry {
  oldStatusCode?: string;
  newStatusCode: string;
  changedBy: string;
  changedAt: string;
  reason?: string;
}

export interface DteStatusResponse {
  id: string;
  controlNumber: string;
  generationCode: string;
  statusCode: string;
  statusName: string;
  mhCode?: string;
  mhMessage?: string;
  submittedAt?: string;
  acceptedAt?: string;
  attemptCount: number;
  nextAttemptAt?: string;
}

export interface DteEventResponse {
  id: string;
  eventTypeCode: string;
  eventLabel: string;
  eventTime: string;
}

export const invoiceApi = {
  list: (
    search = "",
    status = "",
    customerId = "",
    from = "",
    to = "",
    page = 0,
    size = 20
  ) =>
    apiClient
      .get<InvoicePage>("/invoices", {
        params: { search, status, customerId, from, to, page, size },
      })
      .then((r) => r.data),

  get: (id: string) =>
    apiClient.get<InvoiceResponse>(`/invoices/${id}`).then((r) => r.data),

  create: (data: CreateInvoiceRequest) =>
    apiClient.post<InvoiceResponse>("/invoices", data).then((r) => r.data),

  confirm: (id: string) =>
    apiClient.post<InvoiceResponse>(`/invoices/${id}/confirm`).then((r) => r.data),

  cancel: (id: string, reason?: string) =>
    apiClient
      .post<InvoiceResponse>(`/invoices/${id}/cancel`, reason ? { reason } : undefined)
      .then((r) => r.data),

  statusHistory: (id: string) =>
    apiClient
      .get<StatusHistoryEntry[]>(`/invoices/${id}/status-history`)
      .then((r) => r.data),

  dte: (id: string) =>
    apiClient
      .get<DteStatusResponse>(`/invoices/${id}/dte`)
      .then((r) => (r.status === 204 ? null : r.data)),

  dteEvents: (id: string) =>
    apiClient
      .get<DteEventResponse[]>(`/invoices/${id}/dte/events`)
      .then((r) => r.data),

  dteRetry: (id: string) =>
    apiClient
      .post<DteStatusResponse>(`/invoices/${id}/dte/retry`)
      .then((r) => r.data),
};
