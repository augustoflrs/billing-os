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

export const catalogApi = {
  economicActivities: () =>
    apiClient
      .get<EconomicActivityOption[]>("/catalogs/economic-activities")
      .then((r) => r.data),
};
