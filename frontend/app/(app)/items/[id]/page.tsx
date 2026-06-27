"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { itemApi } from "@/lib/api/item";
import { ItemForm } from "@/components/forms/ItemForm";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [addingPrice, setAddingPrice] = useState(false);
  const [newPrice, setNewPrice] = useState("");
  const [newPriceFrom, setNewPriceFrom] = useState(
    new Date().toISOString().slice(0, 16)
  );
  const [priceError, setPriceError] = useState("");

  const { data: item, isLoading } = useQuery({
    queryKey: ["item", id],
    queryFn: () => itemApi.get(id),
  });

  const deactivate = useMutation({
    mutationFn: () => itemApi.deactivate(id),
    onSuccess: () => router.push("/items"),
  });

  const addPrice = useMutation({
    mutationFn: () =>
      itemApi.addPrice(id, {
        unitPrice: parseFloat(newPrice),
        validFrom: new Date(newPriceFrom).toISOString(),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["item", id] });
      setAddingPrice(false);
      setNewPrice("");
      setPriceError("");
    },
    onError: (e: { response?: { data?: { detail?: string } } }) => {
      setPriceError(e?.response?.data?.detail ?? "Error al guardar precio.");
    },
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 p-8">
        <Skeleton className="h-8 w-48" /><Skeleton className="h-64 w-full" />
      </div>
    );
  }
  if (!item) return <div className="p-8 text-muted-foreground">Ítem no encontrado.</div>;

  if (editing) {
    return (
      <div className="mx-auto max-w-2xl p-8">
        <h1 className="mb-6 text-2xl font-bold">Editar ítem</h1>
        <div className="rounded-lg border bg-card p-8 shadow-sm">
          <ItemForm
            existing={item}
            onSuccess={(updated) => {
              qc.setQueryData(["item", id], updated);
              qc.invalidateQueries({ queryKey: ["items"] });
              setEditing(false);
            }}
            onCancel={() => setEditing(false)}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl p-8">
      <div className="mb-6 flex items-start justify-between">
        <div>
          <Link href="/items" className="text-sm text-muted-foreground hover:underline">
            ← Ítems
          </Link>
          <h1 className="mt-1 text-2xl font-bold">{item.name}</h1>
          <div className="mt-1 flex items-center gap-2">
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
              item.itemType === "PRODUCT"
                ? "bg-blue-100 text-blue-800"
                : "bg-purple-100 text-purple-800"
            }`}>
              {item.itemType === "PRODUCT" ? "Producto" : "Servicio"}
            </span>
            {!item.active && (
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                Inactivo
              </span>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          {item.active && (
            <>
              <Button variant="outline" onClick={() => setEditing(true)}>Editar</Button>
              <Button
                variant="ghost" className="text-destructive"
                onClick={() => { if (confirm("¿Desactivar este ítem?")) deactivate.mutate(); }}
              >
                Desactivar
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="rounded-lg border bg-card shadow-sm divide-y">
        {/* Details */}
        <div className="p-6 grid grid-cols-2 gap-4">
          <Field label="SKU" value={item.sku} />
          <Field label="Código interno" value={item.code} />
          {item.description && (
            <div className="col-span-2">
              <p className="text-xs text-muted-foreground">Descripción</p>
              <p className="text-sm">{item.description}</p>
            </div>
          )}
          <Field
            label="Precio actual"
            value={item.currentPrice ? `$${Number(item.currentPrice.unitPrice).toFixed(2)} USD` : undefined}
          />
        </div>

        {/* Price history */}
        <div className="p-6">
          <div className="flex items-center justify-between mb-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Historial de precios
            </p>
            {item.active && !addingPrice && (
              <Button size="sm" variant="outline" onClick={() => setAddingPrice(true)}>
                + Nuevo precio
              </Button>
            )}
          </div>

          {addingPrice && (
            <div className="mb-4 rounded-lg border p-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium mb-1 block">Precio (USD) *</label>
                  <Input
                    type="number" min="0.01" step="0.01" placeholder="0.00"
                    value={newPrice} onChange={(e) => setNewPrice(e.target.value)}
                  />
                </div>
                <div>
                  <label className="text-xs font-medium mb-1 block">Vigente desde *</label>
                  <Input
                    type="datetime-local"
                    value={newPriceFrom} onChange={(e) => setNewPriceFrom(e.target.value)}
                  />
                </div>
              </div>
              {priceError && <p className="text-xs text-destructive">{priceError}</p>}
              <div className="flex justify-end gap-2">
                <Button variant="outline" size="sm"
                  onClick={() => { setAddingPrice(false); setPriceError(""); }}>
                  Cancelar
                </Button>
                <Button size="sm" disabled={!newPrice || addPrice.isPending}
                  onClick={() => addPrice.mutate()}>
                  {addPrice.isPending ? "Guardando..." : "Guardar precio"}
                </Button>
              </div>
            </div>
          )}

          {item.prices.length === 0 ? (
            <p className="text-sm text-muted-foreground">Sin precios registrados.</p>
          ) : (
            <div className="space-y-2">
              {item.prices.map((p) => (
                <div key={p.id} className="flex items-center justify-between rounded-md bg-muted/30 px-3 py-2 text-sm">
                  <span className="font-medium">${Number(p.unitPrice).toFixed(2)} {p.currencyCode}</span>
                  <span className="text-xs text-muted-foreground">
                    Desde {new Date(p.validFrom).toLocaleDateString("es-SV")}
                    {p.validTo ? ` hasta ${new Date(p.validTo).toLocaleDateString("es-SV")}` : " (vigente)"}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm font-medium">{value ?? "—"}</p>
    </div>
  );
}
