"use client";

import { useRouter } from "next/navigation";
import { ItemForm } from "@/components/forms/ItemForm";
import { ItemResponse } from "@/lib/api/item";

export default function NewItemPage() {
  const router = useRouter();
  return (
    <div className="mx-auto max-w-2xl p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Nuevo ítem facturable</h1>
        <p className="mt-1 text-muted-foreground">
          Productos o servicios que aparecerán en tus facturas.
        </p>
      </div>
      <div className="rounded-lg border bg-card p-8 shadow-sm">
        <ItemForm
          onSuccess={(item: ItemResponse) => router.push(`/items/${item.id}`)}
          onCancel={() => router.push("/items")}
        />
      </div>
    </div>
  );
}
