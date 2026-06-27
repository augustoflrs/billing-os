"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { customerApi, CustomerResponse } from "@/lib/api/customer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useDebounce } from "@/lib/hooks/useDebounce";

export default function CustomersPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const { data, isLoading } = useQuery({
    queryKey: ["customers", debouncedSearch, page],
    queryFn: () => customerApi.list(debouncedSearch, page, 20),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="mx-auto max-w-5xl p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Clientes</h1>
          <p className="mt-1 text-muted-foreground">
            {data ? `${data.totalElements} clientes registrados` : ""}
          </p>
        </div>
        <Link href="/customers/new">
          <Button>+ Nuevo cliente</Button>
        </Link>
      </div>

      <div className="mb-4">
        <Input
          placeholder="Buscar por nombre, número o correo..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          className="max-w-sm"
        />
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-16 w-full" />)}
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="px-4 py-3 text-left font-medium">Número</th>
                  <th className="px-4 py-3 text-left font-medium">Nombre</th>
                  <th className="px-4 py-3 text-left font-medium">Correo</th>
                  <th className="px-4 py-3 text-left font-medium">Estado</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {data?.content.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                      Sin clientes{search ? ` para "${search}"` : ""}.
                    </td>
                  </tr>
                )}
                {data?.content.map((c: CustomerResponse) => (
                  <tr key={c.id} className="border-b last:border-0 hover:bg-muted/30">
                    <td className="px-4 py-3 font-mono text-xs">{c.customerNumber}</td>
                    <td className="px-4 py-3">
                      <p className="font-medium">{c.legalName}</p>
                      {c.tradeName && <p className="text-muted-foreground text-xs">{c.tradeName}</p>}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{c.email ?? "—"}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        c.status === "ACTIVE"
                          ? "bg-green-100 text-green-800"
                          : "bg-muted text-muted-foreground"
                      }`}>
                        {c.status === "ACTIVE" ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Link href={`/customers/${c.id}`}>
                        <Button variant="ghost" size="sm">Ver</Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
              <span>Página {data.page + 1} de {data.totalPages}</span>
              <div className="flex gap-2">
                <Button
                  variant="outline" size="sm"
                  disabled={data.page === 0}
                  onClick={() => setPage(p => p - 1)}
                >Anterior</Button>
                <Button
                  variant="outline" size="sm"
                  disabled={data.page >= data.totalPages - 1}
                  onClick={() => setPage(p => p + 1)}
                >Siguiente</Button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
