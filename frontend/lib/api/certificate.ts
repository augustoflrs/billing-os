import { apiClient } from "@/lib/api-client";

export interface CertificateResponse {
  id: string;
  companyId: string;
  alias: string;
  certificatePath: string;
  validFrom: string;
  validTo: string;
  active: boolean;
}

export const certificateApi = {
  list: (companyId: string) =>
    apiClient
      .get<CertificateResponse[]>(`/companies/${companyId}/certificates`)
      .then((r) => r.data),

  upload: (companyId: string, file: File, alias: string, password?: string) => {
    const form = new FormData();
    form.append("file", file);
    form.append("alias", alias);
    if (password) form.append("password", password);
    return apiClient
      .post<CertificateResponse>(`/companies/${companyId}/certificates`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  deactivate: (companyId: string, certId: string) =>
    apiClient.delete(`/companies/${companyId}/certificates/${certId}`),
};
