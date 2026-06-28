"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { dashboardApi, DtePendingAlert, RecentInvoice } from "@/lib/api/dashboard";
import { Skeleton } from "@/components/ui/skeleton";

const STATUS_STYLES: Record<string, string> = {
  ISSUED:      "bg-blue-100 text-blue-800",
  PARTIAL:     "bg-yellow-100 text-yellow-800",
  PAID:        "bg-green-100 text-green-800",
  OVERDUE:     "bg-orange-100 text-orange-800",
  CANCELLED:   "bg-red-100 text-red-700",
};

const DTE_ALERT_STYLES: Record<string, string> = {
  QUEUED:      "bg-blue-100 text-blue-800",
  SUBMITTED:   "bg-yellow-100 text-yellow-800",
  REJECTED:    "bg-red-100 text-red-700",
  CONTINGENCY: "bg-orange-100 text-orange-800",
  BLOCKED:     "bg-gray-200 text-gray-600",
};

function fmt(n: number) {
  return new Intl.NumberFormat("es-SV", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(n);
}

function MetricCard({
  label,
  value,
  sub,
  accent,
}: {
  label: string;
  value: string | number;
  sub?: string;
  accent?: string;
}) {
  return (
    <div className="rounded-lg border bg-card p-5 shadow-sm">
      <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
      </p>
      <p className={`mt-1 text-2xl font-bold ${accent ?? ""}`}>{value}</p>
      {sub && <p className="mt-0.5 text-xs text-muted-foreground">{sub}</p>}
    </div>
  );
}

function StatusBadge({ code, label }: { code: string; label: string }) {
  const cls = STATUS_STYLES[code] ?? "bg-gray-100 text-gray-700";
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${cls}`}>
      {label}
    </span>
  );
}

function DteAlertBadge({ code, label }: { code: string; label: string }) {
  const cls = DTE_ALERT_STYLES[code] ?? "bg-gray-100 text-gray-700";
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${cls}`}>
      {label}
    </span>
  );
}

export default function DashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["dashboard-metrics"],
    queryFn: dashboardApi.metrics,
    refetchInterval: 30_000,
  });

  if (isLoading) {
    return (
      <div className="p-8 space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="p-8 text-sm text-destructive">
        Error al cargar métricas.
      </div>
    );
  }

  const { invoices, dtePendingAlerts, recentInvoices } = data;
  const totalActive = invoices.issued + invoices.partial + invoices.overdue;

  return (
    <div className="p-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Panel Principal</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Resumen de actividad del negocio
        </p>
      </div>

      {/* ── Invoice count cards ── */}
      <div>
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-3">
          Estado de facturas
        </h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          <MetricCard label="Borrador"   value={invoices.draft}     />
          <MetricCard label="Emitidas"   value={invoices.issued}    accent="text-blue-700" />
          <MetricCard label="Pago parcial" value={invoices.partial} accent="text-yellow-700" />
          <MetricCard label="Vencidas"   value={invoices.overdue}   accent="text-orange-600" />
          <MetricCard label="Pagadas"    value={invoices.paid}      accent="text-green-700" />
          <MetricCard label="Anuladas"   value={invoices.cancelled} />
        </div>
      </div>

      {/* ── Financial summary ── */}
      <div>
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-3">
          Resumen financiero
        </h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <MetricCard
            label="Saldo pendiente"
            value={fmt(Number(invoices.outstandingBalance))}
            sub={`${totalActive} factura${totalActive !== 1 ? "s" : ""} activa${totalActive !== 1 ? "s" : ""}`}
            accent="text-orange-600"
          />
          <MetricCard
            label="Facturado este mes"
            value={fmt(Number(invoices.revenueThisMonth))}
          />
          <MetricCard
            label="Cobrado este mes"
            value={fmt(Number(invoices.collectedThisMonth))}
            accent="text-green-700"
          />
        </div>
      </div>

      {/* ── Pending DTE alerts ── */}
      {dtePendingAlerts.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-3 flex items-center gap-2">
            <span className="inline-block h-2 w-2 rounded-full bg-orange-400" />
            DTE pendientes ({dtePendingAlerts.length})
          </h2>
          <div className="rounded-lg border bg-card shadow-sm overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/40 text-muted-foreground text-xs">
                  <th className="px-4 py-2 text-left font-medium">Factura</th>
                  <th className="px-4 py-2 text-left font-medium">Cliente</th>
                  <th className="px-4 py-2 text-left font-medium">Estado DTE</th>
                  <th className="px-4 py-2 text-right font-medium">Intentos</th>
                  <th className="px-4 py-2 text-left font-medium">Próximo intento</th>
                </tr>
              </thead>
              <tbody>
                {dtePendingAlerts.map((alert) => (
                  <DteAlertRow key={alert.invoiceId} alert={alert} />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── Recent invoices ── */}
      {recentInvoices.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-3">
            Facturas recientes
          </h2>
          <div className="rounded-lg border bg-card shadow-sm overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/40 text-muted-foreground text-xs">
                  <th className="px-4 py-2 text-left font-medium">Número</th>
                  <th className="px-4 py-2 text-left font-medium">Cliente</th>
                  <th className="px-4 py-2 text-left font-medium">Estado</th>
                  <th className="px-4 py-2 text-right font-medium">Total</th>
                  <th className="px-4 py-2 text-right font-medium">Saldo</th>
                  <th className="px-4 py-2 text-left font-medium">Fecha</th>
                </tr>
              </thead>
              <tbody>
                {recentInvoices.map((inv) => (
                  <RecentInvoiceRow key={inv.id} inv={inv} />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {dtePendingAlerts.length === 0 && recentInvoices.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No hay facturas aún.{" "}
          <Link href="/invoices/new" className="underline hover:text-foreground">
            Crear la primera
          </Link>
        </p>
      )}
    </div>
  );
}

function DteAlertRow({ alert }: { alert: DtePendingAlert }) {
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30">
      <td className="px-4 py-3">
        <Link
          href={`/invoices/${alert.invoiceId}`}
          className="font-medium hover:underline"
        >
          {alert.invoiceNumber ?? "—"}
        </Link>
      </td>
      <td className="px-4 py-3 text-muted-foreground">{alert.customerName}</td>
      <td className="px-4 py-3">
        <DteAlertBadge code={alert.dteStatusCode} label={alert.dteStatusName} />
      </td>
      <td className="px-4 py-3 text-right tabular-nums">{alert.attemptCount}</td>
      <td className="px-4 py-3 text-muted-foreground">
        {alert.nextAttemptAt
          ? new Date(alert.nextAttemptAt).toLocaleString("es-SV")
          : "—"}
      </td>
    </tr>
  );
}

function RecentInvoiceRow({ inv }: { inv: RecentInvoice }) {
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30">
      <td className="px-4 py-3">
        <Link
          href={`/invoices/${inv.id}`}
          className="font-medium hover:underline"
        >
          {inv.invoiceNumber ?? "—"}
        </Link>
      </td>
      <td className="px-4 py-3 text-muted-foreground">{inv.customerName}</td>
      <td className="px-4 py-3">
        <StatusBadge code={inv.statusCode} label={inv.statusName} />
      </td>
      <td className="px-4 py-3 text-right tabular-nums">
        {fmt(Number(inv.totalAmount))}
      </td>
      <td className="px-4 py-3 text-right tabular-nums text-muted-foreground">
        {Number(inv.balanceAmount) > 0 ? fmt(Number(inv.balanceAmount)) : "—"}
      </td>
      <td className="px-4 py-3 text-muted-foreground">
        {new Date(inv.invoiceDate).toLocaleDateString("es-SV")}
      </td>
    </tr>
  );
}
