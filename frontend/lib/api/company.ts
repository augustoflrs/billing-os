import { apiClient } from "@/lib/api-client";

export interface CompanyResponse {
  id: string;
  legalName: string;
  tradeName?: string;
  nit: string;
  nrc?: string;
  economicActivityCode?: string;
  economicActivityName?: string;
  email?: string;
  phone?: string;
  active: boolean;
  createdAt: string;
  updatedAt?: string;
  version: number;
}

export interface CompanyRequest {
  legalName: string;
  tradeName?: string;
  nit: string;
  nrc?: string;
  economicActivityCode?: string;
  email?: string;
  phone?: string;
}

export const companyApi = {
  get: () =>
    apiClient.get<CompanyResponse>("/companies").then((r) => r.data),

  create: (data: CompanyRequest) =>
    apiClient.post<CompanyResponse>("/companies", data).then((r) => r.data),

  update: (id: string, version: number, data: CompanyRequest) =>
    apiClient
      .put<CompanyResponse>(`/companies/${id}`, data, {
        headers: { "X-Expected-Version": String(version) },
      })
      .then((r) => r.data),
};

export interface EconomicActivityOption {
  code: string;
  name: string;
}

export interface TaxDefinitionOption {
  code: string;
  name: string;
  rate: number;
}

export interface DocumentTypeOption {
  code: string;
  name: string;
}

export const catalogApi = {
  economicActivities: () =>
    apiClient
      .get<EconomicActivityOption[]>("/catalogs/economic-activities")
      .then((r) => r.data),

  taxDefinitions: () =>
    apiClient
      .get<TaxDefinitionOption[]>("/catalogs/tax-definitions")
      .then((r) => r.data),

  documentTypes: () =>
    apiClient
      .get<DocumentTypeOption[]>("/catalogs/document-types")
      .then((r) => r.data),
};
