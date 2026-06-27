import { apiClient } from "@/lib/api-client";

export interface CustomerAddress {
  id: string;
  addressLine1: string;
  departmentCode?: string;
  municipalityCode?: string;
  isDefault: boolean;
}

export interface CustomerTaxProfile {
  id: string;
  documentType: string;
  documentNumber: string;
  nit?: string;
  nrc?: string;
  economicActivityCode?: string;
}

export interface CustomerResponse {
  id: string;
  customerNumber: string;
  legalName: string;
  tradeName?: string;
  email?: string;
  phone?: string;
  status: string;
  createdAt: string;
  updatedAt?: string;
  version: number;
  address?: CustomerAddress;
  taxProfile?: CustomerTaxProfile;
}

export interface CustomerRequest {
  legalName: string;
  tradeName?: string;
  email?: string;
  phone?: string;
  address?: {
    addressLine1: string;
    departmentCode?: string;
    municipalityCode?: string;
  };
  taxProfile?: {
    documentType: string;
    documentNumber: string;
    nit?: string;
    nrc?: string;
    economicActivityCode?: string;
  };
}

export interface CustomerPage {
  content: CustomerResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const customerApi = {
  list: (search = "", page = 0, size = 20) =>
    apiClient
      .get<CustomerPage>("/customers", { params: { search, page, size } })
      .then((r) => r.data),

  get: (id: string) =>
    apiClient.get<CustomerResponse>(`/customers/${id}`).then((r) => r.data),

  create: (data: CustomerRequest) =>
    apiClient.post<CustomerResponse>("/customers", data).then((r) => r.data),

  update: (id: string, version: number, data: CustomerRequest) =>
    apiClient
      .put<CustomerResponse>(`/customers/${id}`, data, {
        headers: { "X-Expected-Version": String(version) },
      })
      .then((r) => r.data),

  deactivate: (id: string) => apiClient.delete(`/customers/${id}`),
};
