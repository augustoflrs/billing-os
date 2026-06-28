"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { invoiceApi, DteStatusResponse, DteEventResponse } from "@/lib/api/invoice";
import { Button } from "@/components/ui/button";

const FINAL_STATUSES = new Set(["ACCEPTED", "REJECTED", "CONTINGENCY", "BLOCKED", "INVALIDATED"]);

const STATUS_STYLES: Record<string, string> = {
  DRAFT:       "bg-gray-100 text-gray-600",
  QUEUED:      "bg-blue-100 text-blue-700",
  SUBMITTED:   "bg-yellow-100 text-yellow-800",
  ACCEPTED:    "bg-green-100 text-green-800",
  REJECTED:    "bg-red-100 text-red-700",
  CONTINGENCY: "bg-orange-100 text-orange-800",
  BLOCKED:     "bg-gray-200 text-gray-600",
  INVALIDATED: "bg-purple-100 text-purple-800",
};

const EVENT_ICONS: Record<string, string> = {
  SUBMISSION:            "→",
  ACCEPTANCE:            "✓",
  REJECTION:             "✗",
  RETRY:                 "↻",
  CONTINGENCY_ENTRY:     "⚠",
  CONTINGENCY_EXIT:      "↩",
  INVALIDATION:          "⊘",
  INVALIDATION_ACCEPTED: "⊘✓",
  INVALIDATION_ERROR:    "⊘✗",
};

function formatTs(ts?: string) {
  if (!ts) return "";
  return new Date(ts).toLocaleString("es-SV", {
    dateStyle: "short",
    timeStyle: "medium",
  });
}

interface Props {
  invoiceId: string;
  invoiceStatusCode: string;
}

export function DteStatusPanel({ invoiceId, invoiceStatusCode }: Props) {
  const qc = useQueryClient();

  // Only relevant for ISSUED+ invoices (a confirmed invoice will eventually have a DTE)
  const shouldShow = !["DRAFT", "CANCELLED"].includes(invoiceStatusCode);
  if (!shouldShow) return null;

  const isFinal = (dte?: DteStatusResponse | null) =>
    dte ? FINAL_STATUSES.has(dte.statusCode) : false;

  const { data: dte, isLoading: dteLoading } = useQuery({
    queryKey: ["invoice-dte", invoiceId],
    queryFn: () => invoiceApi.dte(invoiceId),
    refetchInterval: (query) => (isFinal(query.state.data) ? false : 5_000),
  });

  const { data: events } = useQuery({
    queryKey: ["invoice-dte-events", invoiceId],
    queryFn: () => invoiceApi.dteEvents(invoiceId),
    enabled: !!dte,
    refetchInterval: (query) => {
      const d = qc.getQueryData<DteStatusResponse | null>(["invoice-dte", invoiceId]);
      return isFinal(d) ? false : 5_000;
    },
  });

  const retryMutation = useMutation({
    mutationFn: () => invoiceApi.dteRetry(invoiceId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["invoice-dte", invoiceId] });
      qc.invalidateQueries({ queryKey: ["invoice-dte-events", invoiceId] });
    },
  });

  if (dteLoading) {
    return (
      <div className="mt-6 rounded-lg border p-4 text-sm text-muted-foreground animate-pulse">
        Cargando estado DTE…
      </div>
    );
  }

  if (!dte) {
    return (
      <div className="mt-6 rounded-lg border p-4 text-sm text-muted-foreground">
        DTE aún no generado — procesando…
      </div>
    );
  }

  const canRetry = dte.statusCode === "REJECTED" || dte.statusCode === "CONTINGENCY";

  return (
    <div className="mt-6 rounded-lg border p-4 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs text-muted-foreground font-mono">{dte.controlNumber}</p>
          <p className="text-xs text-muted-foreground">Código: {dte.generationCode}</p>
        </div>
        <div className="flex items-center gap-3">
          <span
            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[dte.statusCode] ?? "bg-gray-100 text-gray-700"}`}
          >
            {dte.statusName}
          </span>
          {canRetry && (
            <Button
              size="sm"
              variant="outline"
              onClick={() => retryMutation.mutate()}
              disabled={retryMutation.isPending}
            >
              {retryMutation.isPending ? "Reintentando…" : "Reintentar"}
            </Button>
          )}
        </div>
      </div>

      {/* MH details */}
      {(dte.mhCode || dte.mhMessage) && (
        <div className="rounded-md bg-muted/50 px-3 py-2 text-xs space-y-0.5">
          {dte.mhCode && <p><span className="font-medium">Código MH:</span> {dte.mhCode}</p>}
          {dte.mhMessage && <p><span className="font-medium">Mensaje:</span> {dte.mhMessage}</p>}
          {dte.acceptedAt && <p><span className="font-medium">Aceptado:</span> {formatTs(dte.acceptedAt)}</p>}
        </div>
      )}

      {/* Retry info */}
      {dte.statusCode === "QUEUED" && dte.attemptCount > 0 && (
        <p className="text-xs text-muted-foreground">
          Intento {dte.attemptCount} — próximo:{" "}
          {dte.nextAttemptAt ? formatTs(dte.nextAttemptAt) : "inmediato"}
        </p>
      )}

      {/* Event timeline */}
      {events && events.length > 0 && (
        <div className="space-y-2 pt-2 border-t">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
            Historial de transmisión
          </p>
          <ol className="space-y-1.5">
            {events.map((ev) => (
              <li key={ev.id} className="flex items-start gap-2 text-xs">
                <span className="mt-0.5 w-4 shrink-0 text-center text-muted-foreground">
                  {EVENT_ICONS[ev.eventTypeCode] ?? "•"}
                </span>
                <div className="flex-1 min-w-0">
                  <span className="font-medium">{ev.eventLabel}</span>
                  <span className="ml-2 text-muted-foreground">{formatTs(ev.eventTime)}</span>
                </div>
              </li>
            ))}
          </ol>
        </div>
      )}
    </div>
  );
}
