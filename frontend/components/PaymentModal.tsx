"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { paymentApi, PAYMENT_METHODS, CreatePaymentRequest } from "@/lib/api/payment";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";

interface Props {
  invoiceId: string;
  invoiceNumber?: string;
  balance: number;
  onClose: () => void;
}

export function PaymentModal({ invoiceId, invoiceNumber, balance, onClose }: Props) {
  const qc = useQueryClient();
  const [method, setMethod] = useState("CASH");
  const [reference, setReference] = useState("");
  const [amount, setAmount] = useState(balance.toFixed(2));

  const mutation = useMutation({
    mutationFn: (data: CreatePaymentRequest) => paymentApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["invoice", invoiceId] });
      qc.invalidateQueries({ queryKey: ["invoice-payments", invoiceId] });
      qc.invalidateQueries({ queryKey: ["invoices"] });
      onClose();
    },
  });

  const parsedAmount = parseFloat(amount);
  const isAmountValid = !isNaN(parsedAmount) && parsedAmount > 0 && parsedAmount <= balance;

  function handleSubmit() {
    if (!isAmountValid) return;
    mutation.mutate({
      paymentMethodCode: method,
      referenceNumber: reference || undefined,
      allocations: [{ invoiceId, amount: parsedAmount }],
    });
  }

  const error = mutation.error as { response?: { data?: { detail?: string } } } | null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-xl border bg-card shadow-lg p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Registrar pago</h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground text-xl leading-none">×</button>
        </div>

        <p className="text-sm text-muted-foreground">
          Factura{" "}
          <span className="font-medium text-foreground">{invoiceNumber ?? invoiceId}</span>
          {" · "}Saldo:{" "}
          <span className="font-medium text-foreground">${balance.toFixed(2)}</span>
        </p>

        <Separator />

        <div className="space-y-3">
          {/* Method */}
          <div className="space-y-1">
            <label className="text-xs font-medium">Método de pago</label>
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              className="w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            >
              {PAYMENT_METHODS.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </select>
          </div>

          {/* Amount */}
          <div className="space-y-1">
            <label className="text-xs font-medium">Monto a abonar ($)</label>
            <Input
              type="number"
              step="0.01"
              min="0.01"
              max={balance}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0.00"
            />
            {!isAmountValid && amount !== "" && (
              <p className="text-xs text-destructive">
                {parsedAmount > balance
                  ? `Máximo permitido: $${balance.toFixed(2)}`
                  : "Ingrese un monto válido"}
              </p>
            )}
          </div>

          {/* Reference */}
          <div className="space-y-1">
            <label className="text-xs font-medium">Referencia (opcional)</label>
            <Input
              value={reference}
              onChange={(e) => setReference(e.target.value)}
              placeholder="Número de transferencia, cheque, etc."
            />
          </div>
        </div>

        {error && (
          <p className="text-xs text-destructive">
            {error.response?.data?.detail ?? "Error al registrar el pago."}
          </p>
        )}

        <div className="flex gap-2 pt-2">
          <Button
            className="flex-1"
            onClick={handleSubmit}
            disabled={!isAmountValid || mutation.isPending}
          >
            {mutation.isPending ? "Registrando…" : "Registrar pago"}
          </Button>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>
            Cancelar
          </Button>
        </div>
      </div>
    </div>
  );
}
