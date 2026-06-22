"use client";

import { useQuery } from "@tanstack/react-query";
import { companyApi } from "@/lib/api/company";
import { CompanyForm } from "@/components/forms/CompanyForm";
import { Skeleton } from "@/components/ui/skeleton";

export default function SettingsCompanyPage() {
  const { data: company, isLoading, error } = useQuery({
    queryKey: ["company"],
    queryFn: companyApi.get,
    retry: false,
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 p-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-4 w-72" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  const notFound =
    !company &&
    (error as { response?: { status?: number } })?.response?.status === 404;

  return (
    <div className="mx-auto max-w-2xl p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Datos de la empresa</h1>
        <p className="mt-1 text-muted-foreground">
          Esta información aparecerá en tus documentos fiscales.
        </p>
      </div>

      {notFound ? (
        <div className="rounded-lg border bg-card p-8 shadow-sm">
          <p className="mb-4 text-muted-foreground">
            No hay empresa configurada aún.
          </p>
          <CompanyForm />
        </div>
      ) : company ? (
        <div className="rounded-lg border bg-card p-8 shadow-sm">
          <CompanyForm existing={company} />
        </div>
      ) : (
        <p className="text-destructive">Error al cargar los datos de la empresa.</p>
      )}
    </div>
  );
}
