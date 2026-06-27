"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { itemApi, ItemResponse } from "@/lib/api/item";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from "@/components/ui/form";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";

const ITEM_TYPES = [
  { value: "PRODUCT", label: "Producto" },
  { value: "SERVICE", label: "Servicio" },
];

const schema = z.object({
  itemType: z.enum(["PRODUCT", "SERVICE"]),
  sku: z.string().max(100).optional().or(z.literal("")),
  code: z.string().max(100).optional().or(z.literal("")),
  name: z.string().min(1, "Requerido").max(255),
  description: z.string().optional().or(z.literal("")),
  hasPrice: z.boolean(),
  unitPrice: z.string().optional().or(z.literal("")),
  validFrom: z.string().optional().or(z.literal("")),
}).superRefine((data, ctx) => {
  if (data.hasPrice) {
    const price = parseFloat(data.unitPrice ?? "");
    if (isNaN(price) || price <= 0) {
      ctx.addIssue({ code: "custom", path: ["unitPrice"], message: "Precio inválido" });
    }
    if (!data.validFrom) {
      ctx.addIssue({ code: "custom", path: ["validFrom"], message: "Requerido" });
    }
  }
});

type FormValues = z.infer<typeof schema>;

interface Props {
  existing?: ItemResponse;
  onSuccess: (item: ItemResponse) => void;
  onCancel?: () => void;
}

export function ItemForm({ existing, onSuccess, onCancel }: Props) {
  const now = new Date().toISOString().slice(0, 16);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      itemType: (existing?.itemType as "PRODUCT" | "SERVICE") ?? "PRODUCT",
      sku: existing?.sku ?? "",
      code: existing?.code ?? "",
      name: existing?.name ?? "",
      description: existing?.description ?? "",
      hasPrice: !existing,
      unitPrice: existing?.currentPrice ? String(existing.currentPrice.unitPrice) : "",
      validFrom: now,
    },
  });

  const hasPrice = form.watch("hasPrice");

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = {
        itemType: values.itemType,
        sku: values.sku || undefined,
        code: values.code || undefined,
        name: values.name,
        description: values.description || undefined,
        price: values.hasPrice && values.unitPrice && values.validFrom ? {
          unitPrice: parseFloat(values.unitPrice),
          validFrom: new Date(values.validFrom).toISOString(),
        } : undefined,
      };
      return existing
        ? itemApi.update(existing.id, existing.version, payload)
        : itemApi.create(payload);
    },
    onSuccess,
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((v) => mutation.mutateAsync(v))} className="space-y-5">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField control={form.control} name="itemType" render={({ field }) => (
            <FormItem>
              <FormLabel>Tipo *</FormLabel>
              <Select onValueChange={field.onChange} value={field.value} disabled={!!existing}>
                <FormControl><SelectTrigger><SelectValue /></SelectTrigger></FormControl>
                <SelectContent>
                  {ITEM_TYPES.map((t) => (
                    <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="name" render={({ field }) => (
            <FormItem>
              <FormLabel>Nombre *</FormLabel>
              <FormControl><Input placeholder="Consultoría / Producto XYZ" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="sku" render={({ field }) => (
            <FormItem>
              <FormLabel>SKU</FormLabel>
              <FormControl><Input placeholder="SKU-001" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="code" render={({ field }) => (
            <FormItem>
              <FormLabel>Código interno</FormLabel>
              <FormControl><Input placeholder="COD-001" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="description" render={({ field }) => (
            <FormItem className="sm:col-span-2">
              <FormLabel>Descripción</FormLabel>
              <FormControl>
                <textarea
                  className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  placeholder="Descripción opcional del ítem"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )} />
        </div>

        <Separator />

        {/* Price section */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <input
              type="checkbox"
              id="hasPrice"
              checked={hasPrice}
              onChange={(e) => form.setValue("hasPrice", e.target.checked)}
              className="h-4 w-4"
            />
            <label htmlFor="hasPrice" className="text-sm font-medium">
              {existing ? "Actualizar precio" : "Establecer precio inicial"}
            </label>
          </div>

          {hasPrice && (
            <div className="grid grid-cols-2 gap-4 pl-6">
              <FormField control={form.control} name="unitPrice" render={({ field }) => (
                <FormItem>
                  <FormLabel>Precio unitario (USD) *</FormLabel>
                  <FormControl>
                    <Input type="number" min="0.01" step="0.01" placeholder="0.00" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="validFrom" render={({ field }) => (
                <FormItem>
                  <FormLabel>Vigente desde *</FormLabel>
                  <FormControl>
                    <Input type="datetime-local" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
            </div>
          )}
        </div>

        {mutation.isError && (
          <p className="text-sm text-destructive">
            {(mutation.error as { response?: { data?: { detail?: string } } })
              ?.response?.data?.detail ?? "Error al guardar."}
          </p>
        )}

        <div className="flex justify-end gap-2">
          {onCancel && (
            <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>
          )}
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando..." : existing ? "Guardar cambios" : "Crear ítem"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
