import { apiClient } from "@/lib/api-client";

export interface AllocationRequest {
  invoiceId: string;
  amount: number;
}

export interface CreatePaymentRequest {
  paymentMethodCode: string;
  referenceNumber?: string;
  paymentDate?: string;
  allocations: AllocationRequest[];
}

export interface AllocationResponse {
  id: string;
  invoiceId: string;
  invoiceNumber?: string;
  allocatedAmount: number;
}

export interface PaymentResponse {
  id: string;
  paymentDate: string;
  amount: number;
  paymentMethodCode: string;
  referenceNumber?: string;
  statusCode: string;
  allocations: AllocationResponse[];
}

export const PAYMENT_METHODS = [
  { value: "CASH",        label: "Efectivo" },
  { value: "TRANSFER",    label: "Transferencia" },
  { value: "CARD_DEBIT",  label: "Tarjeta débito" },
  { value: "CARD_CREDIT", label: "Tarjeta crédito" },
  { value: "CHECK",       label: "Cheque" },
  { value: "CRYPTO",      label: "Criptomoneda" },
  { value: "OTHER",       label: "Otro" },
];

export const paymentApi = {
  create: (data: CreatePaymentRequest) =>
    apiClient.post<PaymentResponse>("/payments", data).then((r) => r.data),

  listByInvoice: (invoiceId: string) =>
    apiClient
      .get<PaymentResponse[]>(`/invoices/${invoiceId}/payments`)
      .then((r) => r.data),
};
