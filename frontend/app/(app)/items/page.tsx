"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { itemApi } from "@/lib/api/item";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useDebounce } from "@/lib/hooks/useDebounce";

export default function ItemsPage() {
  const [search, setSearch] = useState("");
  const [type, setType] = useState("");
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebounce(search, 300);

  const { data, isLoading } = useQuery({
    queryKey: ["items", debouncedSearch, type, page],
    queryFn: () => itemApi.list(debouncedSearch, type, page, 20),
    placeholderData: (prev) => prev,
  });

  return (
    <div className="mx-auto max-w-5xl p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Ítems facturables</h1>
          <p className="mt-1 text-muted-foreground">
            {data ? `${data.totalElements} ítems registrados` : ""}
          </p>
        </div>
        <Link href="/items/new">
          <Button>+ Nuevo ítem</Button>
        </Link>
      </div>

      <div className="mb-4 flex gap-3">
        <Input
          placeholder="Buscar por nombre, SKU o código..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          className="max-w-sm"
        />
        <Select value={type} onValueChange={(v) => { setType(v); setPage(0); }}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Todos" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Todos</SelectItem>
            <SelectItem value="PRODUCT">Productos</SelectItem>
            <SelectItem value="SERVICE">Servicios</SelectItem>
          </SelectContent>
        </Select>
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
                  <th className="px-4 py-3 text-left font-medium">Nombre</th>
                  <th className="px-4 py-3 text-left font-medium">Tipo</th>
                  <th className="px-4 py-3 text-left font-medium">SKU / Código</th>
                  <th className="px-4 py-3 text-right font-medium">Precio actual</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {data?.content.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                      Sin ítems{search ? ` para "${search}"` : ""}.
                    </td>
                  </tr>
                )}
                {data?.content.map((item) => (
                  <tr key={item.id} className="border-b last:border-0 hover:bg-muted/30">
                    <td className="px-4 py-3">
                      <p className="font-medium">{item.name}</p>
                      {item.description && (
                        <p className="text-xs text-muted-foreground line-clamp-1">
                          {item.description}
                        </p>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        item.itemType === "PRODUCT"
                          ? "bg-blue-100 text-blue-800"
                          : "bg-purple-100 text-purple-800"
                      }`}>
                        {item.itemType === "PRODUCT" ? "Producto" : "Servicio"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground font-mono text-xs">
                      {item.sku ?? item.code ?? "—"}
                    </td>
                    <td className="px-4 py-3 text-right font-medium">
                      {item.currentPrice
                        ? `$${Number(item.currentPrice.unitPrice).toFixed(2)}`
                        : <span className="text-muted-foreground">Sin precio</span>}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Link href={`/items/${item.id}`}>
                        <Button variant="ghost" size="sm">Ver</Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {data && data.totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
              <span>Página {data.page + 1} de {data.totalPages}</span>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={data.page === 0}
                  onClick={() => setPage(p => p - 1)}>Anterior</Button>
                <Button variant="outline" size="sm" disabled={data.page >= data.totalPages - 1}
                  onClick={() => setPage(p => p + 1)}>Siguiente</Button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
