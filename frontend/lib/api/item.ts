import { apiClient } from "@/lib/api-client";

export interface ItemPriceResponse {
  id: string;
  unitPrice: number;
  currencyCode: string;
  validFrom: string;
  validTo?: string;
  active: boolean;
}

export interface ItemResponse {
  id: string;
  itemType: string;
  sku?: string;
  code?: string;
  name: string;
  description?: string;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt?: string;
  prices: ItemPriceResponse[];
  currentPrice?: ItemPriceResponse;
}

export interface ItemRequest {
  itemType: string;
  sku?: string;
  code?: string;
  name: string;
  description?: string;
  price?: {
    unitPrice: number;
    validFrom: string;
    validTo?: string;
  };
}

export interface ItemPage {
  content: ItemResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const itemApi = {
  list: (search = "", type = "", page = 0, size = 20) =>
    apiClient
      .get<ItemPage>("/items", { params: { search, type, page, size } })
      .then((r) => r.data),

  get: (id: string) =>
    apiClient.get<ItemResponse>(`/items/${id}`).then((r) => r.data),

  create: (data: ItemRequest) =>
    apiClient.post<ItemResponse>("/items", data).then((r) => r.data),

  update: (id: string, version: number, data: ItemRequest) =>
    apiClient
      .put<ItemResponse>(`/items/${id}`, data, {
        headers: { "X-Expected-Version": String(version) },
      })
      .then((r) => r.data),

  addPrice: (
    id: string,
    price: { unitPrice: number; validFrom: string; validTo?: string }
  ) =>
    apiClient
      .post<ItemPriceResponse>(`/items/${id}/prices`, price)
      .then((r) => r.data),

  deactivate: (id: string) => apiClient.delete(`/items/${id}`),
};
