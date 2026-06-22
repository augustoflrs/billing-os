"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { companyApi } from "@/lib/api/company";
import { branchApi, BranchResponse, posApi, PointOfSaleResponse } from "@/lib/api/branch";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { BranchForm } from "@/components/forms/BranchForm";
import { PointOfSaleForm } from "@/components/forms/PointOfSaleForm";

export default function BranchesPage() {
  const qc = useQueryClient();
  const [editBranch, setEditBranch] = useState<BranchResponse | null>(null);
  const [newBranch, setNewBranch] = useState(false);
  const [selectedBranch, setSelectedBranch] = useState<BranchResponse | null>(null);
  const [editPos, setEditPos] = useState<PointOfSaleResponse | null>(null);
  const [newPos, setNewPos] = useState(false);

  const { data: company, isLoading: loadingCompany } = useQuery({
    queryKey: ["company"],
    queryFn: companyApi.get,
    retry: false,
  });

  const { data: branches = [], isLoading: loadingBranches } = useQuery({
    queryKey: ["branches", company?.id],
    queryFn: () => branchApi.list(company!.id),
    enabled: !!company,
  });

  const { data: posList = [] } = useQuery({
    queryKey: ["pos", company?.id, selectedBranch?.id],
    queryFn: () => posApi.list(company!.id, selectedBranch!.id),
    enabled: !!company && !!selectedBranch,
  });

  const deactivateBranch = useMutation({
    mutationFn: (id: string) => branchApi.deactivate(company!.id, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["branches", company?.id] });
      if (selectedBranch) setSelectedBranch(null);
    },
  });

  const deactivatePos = useMutation({
    mutationFn: (posId: string) =>
      posApi.deactivate(company!.id, selectedBranch!.id, posId),
    onSuccess: () =>
      qc.invalidateQueries({
        queryKey: ["pos", company?.id, selectedBranch?.id],
      }),
  });

  if (loadingCompany || loadingBranches) {
    return (
      <div className="mx-auto max-w-4xl space-y-4 p-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  if (!company) {
    return (
      <div className="p-8 text-muted-foreground">
        Configure su empresa antes de agregar sucursales.
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Sucursales y Puntos de Venta</h1>
          <p className="mt-1 text-muted-foreground">
            Administra las ubicaciones de tu empresa.
          </p>
        </div>
        <Button onClick={() => { setNewBranch(true); setEditBranch(null); }}>
          + Nueva sucursal
        </Button>
      </div>

      {/* New/Edit Branch Form */}
      {(newBranch || editBranch) && (
        <div className="mb-6 rounded-lg border bg-card p-6">
          <h2 className="mb-4 font-semibold">
            {editBranch ? "Editar sucursal" : "Nueva sucursal"}
          </h2>
          <BranchForm
            companyId={company.id}
            existing={editBranch ?? undefined}
            onSuccess={(b) => {
              qc.invalidateQueries({ queryKey: ["branches", company.id] });
              setNewBranch(false);
              setEditBranch(null);
              setSelectedBranch(b);
            }}
            onCancel={() => { setNewBranch(false); setEditBranch(null); }}
          />
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        {/* Branch list */}
        <div className="space-y-3">
          <h2 className="font-semibold text-muted-foreground uppercase text-xs tracking-wide">
            Sucursales
          </h2>
          {branches.length === 0 && (
            <p className="text-sm text-muted-foreground">Sin sucursales aún.</p>
          )}
          {branches.map((b) => (
            <div
              key={b.id}
              className={`cursor-pointer rounded-lg border p-4 transition-colors ${
                selectedBranch?.id === b.id
                  ? "border-primary bg-primary/5"
                  : "hover:bg-accent"
              }`}
              onClick={() => {
                setSelectedBranch(b);
                setNewPos(false);
                setEditPos(null);
              }}
            >
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium">{b.name}</p>
                  <p className="text-sm text-muted-foreground">{b.code} · {b.addressLine1}</p>
                </div>
                <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => { setEditBranch(b); setNewBranch(false); }}
                  >
                    Editar
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive"
                    onClick={() => {
                      if (confirm("¿Desactivar esta sucursal?"))
                        deactivateBranch.mutate(b.id);
                    }}
                  >
                    Desactivar
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* POS panel */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-muted-foreground uppercase text-xs tracking-wide">
              {selectedBranch ? `PDV — ${selectedBranch.name}` : "Puntos de venta"}
            </h2>
            {selectedBranch && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => { setNewPos(true); setEditPos(null); }}
              >
                + Agregar PDV
              </Button>
            )}
          </div>

          {!selectedBranch && (
            <p className="text-sm text-muted-foreground">
              Selecciona una sucursal para ver sus puntos de venta.
            </p>
          )}

          {selectedBranch && (newPos || editPos) && (
            <div className="rounded-lg border bg-card p-4">
              <PointOfSaleForm
                companyId={company.id}
                branchId={selectedBranch.id}
                existing={editPos ?? undefined}
                onSuccess={() => {
                  qc.invalidateQueries({
                    queryKey: ["pos", company.id, selectedBranch.id],
                  });
                  setNewPos(false);
                  setEditPos(null);
                }}
                onCancel={() => { setNewPos(false); setEditPos(null); }}
              />
            </div>
          )}

          {selectedBranch && posList.map((pos) => (
            <div key={pos.id} className="rounded-lg border p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">{pos.name}</p>
                  <p className="text-sm text-muted-foreground">{pos.code}</p>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => { setEditPos(pos); setNewPos(false); }}
                  >
                    Editar
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive"
                    onClick={() => {
                      if (confirm("¿Desactivar este PDV?"))
                        deactivatePos.mutate(pos.id);
                    }}
                  >
                    Desactivar
                  </Button>
                </div>
              </div>
            </div>
          ))}

          {selectedBranch && !newPos && !editPos && posList.length === 0 && (
            <p className="text-sm text-muted-foreground">Sin puntos de venta aún.</p>
          )}
        </div>
      </div>
    </div>
  );
}
