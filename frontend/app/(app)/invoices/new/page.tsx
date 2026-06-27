"use client";

import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { InvoiceForm } from "@/components/forms/InvoiceForm";
import { InvoiceResponse } from "@/lib/api/invoice";
import { companyApi } from "@/lib/api/company";
import { Skeleton } from "@/components/ui/skeleton";

export default function NewInvoicePage() {
  const router = useRouter();

  const { data: company, isLoading } = useQuery({
    queryKey: ["company"],
    queryFn: () => companyApi.get(),
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl p-8 space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!company) {
    return (
      <div className="p-8 text-muted-foreground">
        Configure la empresa antes de crear facturas.
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Nueva factura</h1>
        <p className="mt-1 text-muted-foreground">
          Crea una factura en estado borrador. Confírmala para emitirla.
        </p>
      </div>
      <div className="rounded-lg border bg-card p-8 shadow-sm">
        <InvoiceForm
          companyId={company.id}
          onSuccess={(invoice: InvoiceResponse) =>
            router.push(`/invoices/${invoice.id}`)
          }
          onCancel={() => router.push("/invoices")}
        />
      </div>
    </div>
  );
}
