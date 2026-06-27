"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { customerApi } from "@/lib/api/customer";
import { CustomerForm } from "@/components/forms/CustomerForm";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";

export default function CustomerDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);

  const { data: customer, isLoading } = useQuery({
    queryKey: ["customer", id],
    queryFn: () => customerApi.get(id),
  });

  const deactivate = useMutation({
    mutationFn: () => customerApi.deactivate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["customer", id] });
      qc.invalidateQueries({ queryKey: ["customers"] });
    },
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 p-8">
        <Skeleton className="h-8 w-48" /><Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!customer) return <div className="p-8 text-muted-foreground">Cliente no encontrado.</div>;

  if (editing) {
    return (
      <div className="mx-auto max-w-2xl p-8">
        <h1 className="mb-6 text-2xl font-bold">Editar cliente</h1>
        <div className="rounded-lg border bg-card p-8 shadow-sm">
          <CustomerForm
            existing={customer}
            onSuccess={(c) => {
              qc.setQueryData(["customer", id], c);
              qc.invalidateQueries({ queryKey: ["customers"] });
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
      <div className="mb-6 flex items-center justify-between">
        <div>
          <Link href="/customers" className="text-sm text-muted-foreground hover:underline">
            ← Clientes
          </Link>
          <h1 className="mt-1 text-2xl font-bold">{customer.legalName}</h1>
          <p className="text-sm text-muted-foreground">{customer.customerNumber}</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setEditing(true)}>Editar</Button>
          {customer.status === "ACTIVE" && (
            <Button
              variant="ghost"
              className="text-destructive"
              onClick={() => {
                if (confirm("¿Desactivar este cliente?")) deactivate.mutate();
              }}
            >
              Desactivar
            </Button>
          )}
        </div>
      </div>

      <div className="rounded-lg border bg-card shadow-sm divide-y">
        {/* Basic info */}
        <div className="p-6 grid grid-cols-2 gap-4">
          <Field label="Nombre comercial" value={customer.tradeName} />
          <Field label="Correo" value={customer.email} />
          <Field label="Teléfono" value={customer.phone} />
          <Field label="Estado" value={customer.status === "ACTIVE" ? "Activo" : "Inactivo"} />
        </div>

        {/* Address */}
        {customer.address && (
          <div className="p-6">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-3">
              Dirección
            </p>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Dirección" value={customer.address.addressLine1} />
              <Field label="Departamento" value={customer.address.departmentCode} />
              <Field label="Municipio" value={customer.address.municipalityCode} />
            </div>
          </div>
        )}

        {/* Tax profile */}
        {customer.taxProfile && (
          <div className="p-6">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-3">
              Perfil fiscal
            </p>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Tipo documento" value={customer.taxProfile.documentType} />
              <Field label="Número documento" value={customer.taxProfile.documentNumber} />
              <Field label="NIT" value={customer.taxProfile.nit} />
              <Field label="NRC" value={customer.taxProfile.nrc} />
            </div>
          </div>
        )}
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
