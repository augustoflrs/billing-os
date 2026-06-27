"use client";

import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { invoiceApi, InvoiceResponse } from "@/lib/api/invoice";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";

const STATUS_STYLES: Record<string, string> = {
  DRAFT:     "bg-gray-100 text-gray-700",
  ISSUED:    "bg-blue-100 text-blue-800",
  PARTIAL:   "bg-yellow-100 text-yellow-800",
  PAID:      "bg-green-100 text-green-800",
  CANCELLED: "bg-red-100 text-red-700",
  OVERDUE:   "bg-orange-100 text-orange-800",
};

export default function InvoiceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const qc = useQueryClient();

  const { data: invoice, isLoading } = useQuery({
    queryKey: ["invoice", id],
    queryFn: () => invoiceApi.get(id),
  });

  const confirmMutation = useMutation({
    mutationFn: () => invoiceApi.confirm(id),
    onSuccess: (updated) => {
      qc.setQueryData(["invoice", id], updated);
      qc.invalidateQueries({ queryKey: ["invoices"] });
    },
  });

  const cancel = useMutation({
    mutationFn: () => invoiceApi.cancel(id),
    onSuccess: (updated) => {
      qc.setQueryData(["invoice", id], updated);
      qc.invalidateQueries({ queryKey: ["invoices"] });
    },
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-4 p-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }
  if (!invoice) {
    return <div className="p-8 text-muted-foreground">Factura no encontrada.</div>;
  }

  const statusStyle =
    STATUS_STYLES[invoice.statusCode] ?? "bg-gray-100 text-gray-700";
  const isDraft = invoice.statusCode === "DRAFT";
  const canCancel = !["CANCELLED", "PAID"].includes(invoice.statusCode);

  return (
    <div className="mx-auto max-w-3xl p-8">
      {/* Header */}
      <div className="mb-6 flex items-start justify-between">
        <div>
          <Link
            href="/invoices"
            className="text-sm text-muted-foreground hover:underline"
          >
            ← Facturas
          </Link>
          <h1 className="mt-1 text-2xl font-bold">
            {invoice.invoiceNumber ?? "Factura borrador"}
          </h1>
          <div className="mt-1 flex items-center gap-2">
            <span
              className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusStyle}`}
            >
              {invoice.statusName}
            </span>
            <span className="text-xs text-muted-foreground">
              {invoice.documentTypeCode} ·{" "}
              {new Date(invoice.invoiceDate).toLocaleDateString("es-SV")}
            </span>
          </div>
        </div>
        <div className="flex gap-2">
          {isDraft && (
            <Button
              onClick={() => confirmMutation.mutate()}
              disabled={confirmMutation.isPending}
            >
              {confirmMutation.isPending ? "Emitiendo..." : "Emitir factura"}
            </Button>
          )}
          {canCancel && (
            <Button
              variant="ghost"
              className="text-destructive"
              onClick={() => {
                if (confirm("¿Anular esta factura?")) cancel.mutate();
              }}
              disabled={cancel.isPending}
            >
              Anular
            </Button>
          )}
        </div>
      </div>

      {/* Confirm/cancel errors */}
      {confirmMutation.isError && (
        <p className="mb-4 text-sm text-destructive">
          {(confirmMutation.error as { response?: { data?: { detail?: string } } })
            ?.response?.data?.detail ?? "Error al emitir."}
        </p>
      )}
      {cancel.isError && (
        <p className="mb-4 text-sm text-destructive">
          {(cancel.error as { response?: { data?: { detail?: string } } })
            ?.response?.data?.detail ?? "Error al anular."}
        </p>
      )}

      <div className="rounded-lg border bg-card shadow-sm divide-y">
        {/* Summary */}
        <div className="p-6 grid grid-cols-2 gap-4 text-sm">
          <Field label="Cliente" value={invoice.customerName} />
          <Field label="Punto de venta" value={invoice.pointOfSaleId} />
          <Field
            label="Fecha emisión"
            value={new Date(invoice.invoiceDate).toLocaleString("es-SV")}
          />
          <Field label="Tipo documento" value={invoice.documentTypeCode} />
        </div>

        {/* Lines */}
        <div className="p-6">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-3">
            Líneas
          </p>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-muted-foreground">
                <th className="pb-2 text-left font-medium">Descripción</th>
                <th className="pb-2 text-right font-medium">Cant.</th>
                <th className="pb-2 text-right font-medium">P. Unit.</th>
                <th className="pb-2 text-right font-medium">Desc.</th>
                <th className="pb-2 text-right font-medium">Imp.</th>
                <th className="pb-2 text-right font-medium">Total</th>
              </tr>
            </thead>
            <tbody>
              {invoice.lines.map((line) => (
                <tr key={line.id} className="border-b last:border-0">
                  <td className="py-2">
                    <p className="font-medium">{line.itemName}</p>
                    {line.itemDescription && (
                      <p className="text-xs text-muted-foreground">
                        {line.itemDescription}
                      </p>
                    )}
                    {line.taxes.map((t) => (
                      <span
                        key={t.taxCode}
                        className="text-xs text-muted-foreground"
                      >
                        {t.taxCode} {(Number(t.rate) * 100).toFixed(0)}%
                      </span>
                    ))}
                  </td>
                  <td className="py-2 text-right">{Number(line.quantity).toFixed(2)}</td>
                  <td className="py-2 text-right">
                    ${Number(line.unitPrice).toFixed(2)}
                  </td>
                  <td className="py-2 text-right">
                    {Number(line.discountAmount) > 0
                      ? `-$${Number(line.discountAmount).toFixed(2)}`
                      : "—"}
                  </td>
                  <td className="py-2 text-right">
                    {Number(line.taxAmount) > 0
                      ? `$${Number(line.taxAmount).toFixed(2)}`
                      : "—"}
                  </td>
                  <td className="py-2 text-right font-medium">
                    ${Number(line.totalAmount).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Totals */}
        <div className="p-6">
          <div className="ml-auto max-w-xs space-y-1 text-sm">
            <TotalRow label="Subtotal" value={invoice.subtotalAmount} />
            {Number(invoice.discountAmount) > 0 && (
              <TotalRow
                label="Descuentos"
                value={-Number(invoice.discountAmount)}
              />
            )}
            <TotalRow label="Impuestos" value={invoice.taxAmount} />
            <Separator className="my-1" />
            <TotalRow label="Total" value={invoice.totalAmount} bold />
            <TotalRow label="Abonado" value={invoice.paidAmount} />
            <TotalRow label="Saldo" value={invoice.balanceAmount} />
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="font-medium">{value ?? "—"}</p>
    </div>
  );
}

function TotalRow({
  label,
  value,
  bold,
}: {
  label: string;
  value: number | string;
  bold?: boolean;
}) {
  const cls = bold ? "font-bold text-base" : "";
  return (
    <div className={`flex justify-between ${cls}`}>
      <span className={bold ? "" : "text-muted-foreground"}>{label}</span>
      <span>${Math.abs(Number(value)).toFixed(2)}</span>
    </div>
  );
}
