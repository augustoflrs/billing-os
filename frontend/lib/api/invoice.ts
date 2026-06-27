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

export const invoiceApi = {
  list: (search = "", page = 0, size = 20) =>
    apiClient
      .get<InvoicePage>("/invoices", { params: { search, page, size } })
      .then((r) => r.data),

  get: (id: string) =>
    apiClient.get<InvoiceResponse>(`/invoices/${id}`).then((r) => r.data),

  create: (data: CreateInvoiceRequest) =>
    apiClient.post<InvoiceResponse>("/invoices", data).then((r) => r.data),

  confirm: (id: string) =>
    apiClient.post<InvoiceResponse>(`/invoices/${id}/confirm`).then((r) => r.data),

  cancel: (id: string) =>
    apiClient.post<InvoiceResponse>(`/invoices/${id}/cancel`).then((r) => r.data),
};
