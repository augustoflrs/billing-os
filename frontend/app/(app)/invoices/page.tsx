"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { invoiceApi, InvoiceSummary } from "@/lib/api/invoice";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useDebounce } from "@/lib/hooks/useDebounce";

const STATUS_STYLES: Record<string, string> = {
  DRAFT:     "bg-gray-100 text-gray-700",
  ISSUED:    "bg-blue-100 text-blue-800",
  PARTIAL:   "bg-yellow-100 text-yellow-800",
  PAID:      "bg-green-100 text-green-800",
  CANCELLED: "bg-red-100 text-red-700",
  OVERDUE:   "bg-orange-100 text-orange-800",
};

export default function InvoicesPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const { data, isLoading } = useQuery({
    queryKey: ["invoices", debouncedSearch, page],
    queryFn: () => invoiceApi.list(debouncedSearch, page, 20),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="mx-auto max-w-5xl p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Facturas</h1>
          <p className="mt-1 text-muted-foreground">
            {data ? `${data.totalElements} facturas registradas` : ""}
          </p>
        </div>
        <Link href="/invoices/new">
          <Button>+ Nueva factura</Button>
        </Link>
      </div>

      <div className="mb-4">
        <Input
          placeholder="Buscar por número de factura o cliente..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          className="max-w-sm"
        />
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="px-4 py-3 text-left font-medium">N° Factura</th>
                  <th className="px-4 py-3 text-left font-medium">Cliente</th>
                  <th className="px-4 py-3 text-left font-medium">Tipo</th>
                  <th className="px-4 py-3 text-left font-medium">Fecha</th>
                  <th className="px-4 py-3 text-right font-medium">Total</th>
                  <th className="px-4 py-3 text-left font-medium">Estado</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {data?.content.length === 0 && (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-8 text-center text-muted-foreground"
                    >
                      Sin facturas{search ? ` para "${search}"` : ""}.
                    </td>
                  </tr>
                )}
                {data?.content.map((inv) => (
                  <InvoiceRow key={inv.id} inv={inv} />
                ))}
              </tbody>
            </table>
          </div>

          {data && data.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
              <span>
                Página {data.page + 1} de {data.totalPages}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  Anterior
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.page >= data.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Siguiente
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function InvoiceRow({ inv }: { inv: InvoiceSummary }) {
  const style = STATUS_STYLES[inv.statusCode] ?? "bg-gray-100 text-gray-700";
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30">
      <td className="px-4 py-3 font-mono text-xs">
        {inv.invoiceNumber ?? <span className="text-muted-foreground italic">Borrador</span>}
      </td>
      <td className="px-4 py-3">
        <p className="font-medium">{inv.customerName}</p>
      </td>
      <td className="px-4 py-3 text-muted-foreground">{inv.documentTypeCode}</td>
      <td className="px-4 py-3 text-muted-foreground">
        {new Date(inv.invoiceDate).toLocaleDateString("es-SV")}
      </td>
      <td className="px-4 py-3 text-right font-medium">
        ${Number(inv.totalAmount).toFixed(2)}
      </td>
      <td className="px-4 py-3">
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${style}`}>
          {inv.statusName}
        </span>
      </td>
      <td className="px-4 py-3 text-right">
        <Link href={`/invoices/${inv.id}`}>
          <Button variant="ghost" size="sm">
            Ver
          </Button>
        </Link>
      </td>
    </tr>
  );
}
