import { apiClient } from "@/lib/api-client";

export interface BranchResponse {
  id: string;
  companyId: string;
  code: string;
  name: string;
  addressLine1: string;
  departmentCode?: string;
  municipalityCode?: string;
  phone?: string;
  active: boolean;
}

export interface BranchRequest {
  code: string;
  name: string;
  addressLine1: string;
  departmentCode?: string;
  municipalityCode?: string;
  phone?: string;
}

export interface PointOfSaleResponse {
  id: string;
  branchId: string;
  code: string;
  name: string;
  active: boolean;
}

export interface PointOfSaleRequest {
  code: string;
  name: string;
}

export const branchApi = {
  list: (companyId: string) =>
    apiClient
      .get<BranchResponse[]>(`/companies/${companyId}/branches`)
      .then((r) => r.data),

  create: (companyId: string, data: BranchRequest) =>
    apiClient
      .post<BranchResponse>(`/companies/${companyId}/branches`, data)
      .then((r) => r.data),

  update: (companyId: string, branchId: string, data: BranchRequest) =>
    apiClient
      .put<BranchResponse>(`/companies/${companyId}/branches/${branchId}`, data)
      .then((r) => r.data),

  deactivate: (companyId: string, branchId: string) =>
    apiClient.delete(`/companies/${companyId}/branches/${branchId}`),
};

export const posApi = {
  list: (companyId: string, branchId: string) =>
    apiClient
      .get<PointOfSaleResponse[]>(
        `/companies/${companyId}/branches/${branchId}/pos`
      )
      .then((r) => r.data),

  create: (companyId: string, branchId: string, data: PointOfSaleRequest) =>
    apiClient
      .post<PointOfSaleResponse>(
        `/companies/${companyId}/branches/${branchId}/pos`,
        data
      )
      .then((r) => r.data),

  update: (
    companyId: string,
    branchId: string,
    posId: string,
    data: PointOfSaleRequest
  ) =>
    apiClient
      .put<PointOfSaleResponse>(
        `/companies/${companyId}/branches/${branchId}/pos/${posId}`,
        data
      )
      .then((r) => r.data),

  deactivate: (companyId: string, branchId: string, posId: string) =>
    apiClient.delete(
      `/companies/${companyId}/branches/${branchId}/pos/${posId}`
    ),
};
