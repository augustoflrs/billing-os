"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { invoiceApi, InvoiceResponse } from "@/lib/api/invoice";
import { paymentApi, PaymentResponse } from "@/lib/api/payment";
import { DteStatusPanel } from "@/components/DteStatusPanel";
import { PaymentModal } from "@/components/PaymentModal";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  const qc = useQueryClient();
  const [cancelReason, setCancelReason] = useState("");
  const [showCancelForm, setShowCancelForm] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);

  const { data: invoice, isLoading } = useQuery({
    queryKey: ["invoice", id],
    queryFn: () => invoiceApi.get(id),
  });

  const { data: history } = useQuery({
    queryKey: ["invoice-history", id],
    queryFn: () => invoiceApi.statusHistory(id),
    enabled: !!invoice,
  });

  const { data: payments } = useQuery({
    queryKey: ["invoice-payments", id],
    queryFn: () => paymentApi.listByInvoice(id),
    enabled: !!invoice,
  });

  const { data: pdfUrl } = useQuery({
    queryKey: ["invoice-pdf", id],
    queryFn: () => invoiceApi.pdfUrl(id),
    enabled: !!invoice && !["DRAFT", "CANCELLED"].includes(invoice.statusCode),
    retry: false,
    refetchOnWindowFocus: false,
  });

  const confirmMutation = useMutation({
    mutationFn: () => invoiceApi.confirm(id),
    onSuccess: (updated) => {
      qc.setQueryData(["invoice", id], updated);
      qc.invalidateQueries({ queryKey: ["invoices"] });
      qc.invalidateQueries({ queryKey: ["invoice-history", id] });
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => invoiceApi.cancel(id, cancelReason || undefined),
    onSuccess: (updated) => {
      qc.setQueryData(["invoice", id], updated);
      qc.invalidateQueries({ queryKey: ["invoices"] });
      qc.invalidateQueries({ queryKey: ["invoice-history", id] });
      setShowCancelForm(false);
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

  const statusStyle = STATUS_STYLES[invoice.statusCode] ?? "bg-gray-100 text-gray-700";
  const isDraft    = invoice.statusCode === "DRAFT";
  const canCancel  = !["CANCELLED", "PAID", "DRAFT"].includes(invoice.statusCode);
  const canPay     = ["ISSUED", "PARTIAL", "OVERDUE"].includes(invoice.statusCode)
                     && Number(invoice.balanceAmount) > 0;

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
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusStyle}`}>
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
          {pdfUrl && (
            <a
              href={pdfUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium shadow-sm hover:bg-accent hover:text-accent-foreground"
            >
              Descargar PDF
            </a>
          )}
          {canPay && (
            <Button variant="outline" onClick={() => setShowPaymentModal(true)}>
              Registrar pago
            </Button>
          )}
          {canCancel && !showCancelForm && (
            <Button
              variant="ghost"
              className="text-destructive"
              onClick={() => setShowCancelForm(true)}
            >
              Anular
            </Button>
          )}
        </div>
      </div>

      {/* Error banners */}
      {confirmMutation.isError && (
        <p className="mb-4 text-sm text-destructive">
          {(confirmMutation.error as { response?: { data?: { detail?: string } } })
            ?.response?.data?.detail ?? "Error al emitir."}
        </p>
      )}

      {/* Cancel form */}
      {showCancelForm && (
        <div className="mb-4 rounded-lg border border-destructive/30 bg-destructive/5 p-4 space-y-3">
          <p className="text-sm font-medium">Anular factura</p>
          <Input
            placeholder="Motivo de anulación (opcional)"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
          />
          {cancelMutation.isError && (
            <p className="text-xs text-destructive">
              {(cancelMutation.error as { response?: { data?: { detail?: string } } })
                ?.response?.data?.detail ?? "Error al anular."}
            </p>
          )}
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="destructive"
              onClick={() => cancelMutation.mutate()}
              disabled={cancelMutation.isPending}
            >
              {cancelMutation.isPending ? "Anulando..." : "Confirmar anulación"}
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => { setShowCancelForm(false); setCancelReason(""); }}
            >
              Cancelar
            </Button>
          </div>
        </div>
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
                      <span key={t.taxCode} className="text-xs text-blue-600">
                        {t.taxCode} {(Number(t.rate) * 100).toFixed(0)}%{" "}
                      </span>
                    ))}
                  </td>
                  <td className="py-2 text-right">{Number(line.quantity).toFixed(2)}</td>
                  <td className="py-2 text-right">${Number(line.unitPrice).toFixed(2)}</td>
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
              <TotalRow label="Descuentos" value={invoice.discountAmount} negate />
            )}
            <TotalRow label="Impuestos" value={invoice.taxAmount} />
            <Separator className="my-1" />
            <TotalRow label="Total" value={invoice.totalAmount} bold />
            <TotalRow label="Abonado" value={invoice.paidAmount} />
            <TotalRow label="Saldo pendiente" value={invoice.balanceAmount} />
          </div>
        </div>

        {/* Payment history */}
        {payments && payments.length > 0 && (
          <div className="p-6">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-3">
              Pagos recibidos
            </p>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-muted-foreground text-xs">
                  <th className="pb-2 text-left font-medium">Fecha</th>
                  <th className="pb-2 text-left font-medium">Método</th>
                  <th className="pb-2 text-left font-medium">Referencia</th>
                  <th className="pb-2 text-right font-medium">Monto</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id} className="border-b last:border-0">
                    <td className="py-2">
                      {new Date(p.paymentDate).toLocaleDateString("es-SV")}
                    </td>
                    <td className="py-2">{p.paymentMethodCode}</td>
                    <td className="py-2 text-muted-foreground">{p.referenceNumber ?? "—"}</td>
                    <td className="py-2 text-right font-medium">
                      ${Number(p.amount).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Status history */}
        {history && history.length > 0 && (
          <div className="p-6">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-3">
              Historial de estado
            </p>
            <div className="space-y-2">
              {history.map((h, i) => (
                <div key={i} className="flex items-start gap-3 text-sm">
                  <div className="mt-1 h-2 w-2 rounded-full bg-muted-foreground/40 flex-shrink-0" />
                  <div>
                    <span className="font-medium">
                      {h.oldStatusCode
                        ? `${h.oldStatusCode} → ${h.newStatusCode}`
                        : h.newStatusCode}
                    </span>
                    {h.reason && (
                      <span className="ml-2 text-muted-foreground">— {h.reason}</span>
                    )}
                    <p className="text-xs text-muted-foreground">
                      {h.changedBy} · {new Date(h.changedAt).toLocaleString("es-SV")}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* DTE status + event timeline */}
      <DteStatusPanel invoiceId={id} invoiceStatusCode={invoice.statusCode} />

      {/* Payment modal */}
      {showPaymentModal && (
        <PaymentModal
          invoiceId={id}
          invoiceNumber={invoice.invoiceNumber}
          balance={Number(invoice.balanceAmount)}
          onClose={() => setShowPaymentModal(false)}
        />
      )}
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
  negate,
}: {
  label: string;
  value: number | string;
  bold?: boolean;
  negate?: boolean;
}) {
  const n = Number(value);
  const display = negate ? `-$${n.toFixed(2)}` : `$${n.toFixed(2)}`;
  return (
    <div className={`flex justify-between ${bold ? "font-bold text-base border-t pt-1" : ""}`}>
      <span className={bold ? "" : "text-muted-foreground"}>{label}</span>
      <span>{display}</span>
    </div>
  );
}
