"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { invoiceApi, InvoiceSummary } from "@/lib/api/invoice";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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

const STATUS_OPTIONS = [
  { value: "", label: "Todos los estados" },
  { value: "DRAFT",     label: "Borrador" },
  { value: "ISSUED",    label: "Emitida" },
  { value: "PARTIAL",   label: "Pago Parcial" },
  { value: "PAID",      label: "Pagada" },
  { value: "CANCELLED", label: "Anulada" },
  { value: "OVERDUE",   label: "Vencida" },
];

export default function InvoicesPage() {
  const [search, setSearch]   = useState("");
  const [status, setStatus]   = useState("");
  const [from, setFrom]       = useState("");
  const [to, setTo]           = useState("");
  const [page, setPage]       = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const resetPage = () => setPage(0);

  const { data, isLoading } = useQuery({
    queryKey: ["invoices", debouncedSearch, status, from, to, page],
    queryFn: () => invoiceApi.list(debouncedSearch, status, "", from, to, page, 20),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="mx-auto max-w-6xl p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Facturas</h1>
          <p className="mt-1 text-muted-foreground">
            {data ? `${data.totalElements} facturas` : ""}
          </p>
        </div>
        <Link href="/invoices/new">
          <Button>+ Nueva factura</Button>
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-wrap gap-3">
        <Input
          placeholder="Buscar por número o cliente..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); resetPage(); }}
          className="max-w-xs"
        />
        <Select value={status} onValueChange={(v) => { setStatus(v); resetPage(); }}>
          <SelectTrigger className="w-44">
            <SelectValue placeholder="Estado" />
          </SelectTrigger>
          <SelectContent>
            {STATUS_OPTIONS.map((s) => (
              <SelectItem key={s.value} value={s.value}>
                {s.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div className="flex items-center gap-1">
          <Input
            type="date"
            value={from}
            onChange={(e) => { setFrom(e.target.value); resetPage(); }}
            className="w-36"
          />
          <span className="text-muted-foreground text-sm">–</span>
          <Input
            type="date"
            value={to}
            onChange={(e) => { setTo(e.target.value); resetPage(); }}
            className="w-36"
          />
        </div>
        {(search || status || from || to) && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => { setSearch(""); setStatus(""); setFrom(""); setTo(""); resetPage(); }}
          >
            Limpiar filtros
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} className="h-14 w-full" />
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
                  <th className="px-4 py-3 text-right font-medium">Saldo</th>
                  <th className="px-4 py-3 text-left font-medium">Estado</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {data?.content.length === 0 && (
                  <tr>
                    <td
                      colSpan={8}
                      className="px-4 py-10 text-center text-muted-foreground"
                    >
                      No se encontraron facturas con los filtros actuales.
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
  const balance = Number(inv.balanceAmount);
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30">
      <td className="px-4 py-3 font-mono text-xs">
        {inv.invoiceNumber ?? (
          <span className="text-muted-foreground italic">Borrador</span>
        )}
      </td>
      <td className="px-4 py-3 font-medium">{inv.customerName}</td>
      <td className="px-4 py-3 text-muted-foreground">{inv.documentTypeCode}</td>
      <td className="px-4 py-3 text-muted-foreground">
        {new Date(inv.invoiceDate).toLocaleDateString("es-SV")}
      </td>
      <td className="px-4 py-3 text-right font-medium">
        ${Number(inv.totalAmount).toFixed(2)}
      </td>
      <td className="px-4 py-3 text-right">
        {balance > 0 ? (
          <span className="font-medium text-orange-600">${balance.toFixed(2)}</span>
        ) : (
          <span className="text-green-600">$0.00</span>
        )}
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
