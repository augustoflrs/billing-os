"use client";

import { useState, useCallback } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { useForm, useFieldArray, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { invoiceApi, InvoiceResponse } from "@/lib/api/invoice";
import { customerApi } from "@/lib/api/customer";
import { branchApi, posApi } from "@/lib/api/branch";
import { itemApi } from "@/lib/api/item";
import { catalogApi } from "@/lib/api/company";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { useDebounce } from "@/lib/hooks/useDebounce";

const lineSchema = z.object({
  billableItemId: z.string().optional(),
  itemName: z.string().min(1, "Requerido"),
  itemDescription: z.string().optional(),
  quantity: z.string().min(1, "Requerido"),
  unitPrice: z.string().min(1, "Requerido"),
  discountAmount: z.string().optional(),
  taxCode: z.string().optional(),
});

const schema = z.object({
  customerId: z.string().min(1, "Seleccione un cliente"),
  branchId: z.string().min(1, "Seleccione una sucursal"),
  pointOfSaleId: z.string().min(1, "Seleccione un punto de venta"),
  documentTypeCode: z.string().min(1, "Seleccione tipo de documento"),
  invoiceDate: z.string().min(1, "Requerido"),
  lines: z.array(lineSchema).min(1, "Agregue al menos una línea"),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  companyId: string;
  onSuccess: (invoice: InvoiceResponse) => void;
  onCancel?: () => void;
}

const emptyLine = () => ({
  billableItemId: "",
  itemName: "",
  itemDescription: "",
  quantity: "1",
  unitPrice: "",
  discountAmount: "",
  taxCode: "IVA",
});

export function InvoiceForm({ companyId, onSuccess, onCancel }: Props) {
  const [customerSearch, setCustomerSearch] = useState("");
  const [itemSearches, setItemSearches] = useState<Record<number, string>>({});
  const debouncedCustomer = useDebounce(customerSearch, 300);

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      customerId: "",
      branchId: "",
      pointOfSaleId: "",
      documentTypeCode: "01",
      invoiceDate: new Date().toISOString().slice(0, 16),
      lines: [emptyLine()],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "lines",
  });

  const branchId = form.watch("branchId");

  const { data: customers } = useQuery({
    queryKey: ["customers-search", debouncedCustomer],
    queryFn: () => customerApi.list(debouncedCustomer, 0, 10),
    enabled: debouncedCustomer.length > 0,
  });

  const { data: branches } = useQuery({
    queryKey: ["branches", companyId],
    queryFn: () => branchApi.list(companyId),
  });

  const { data: posList } = useQuery({
    queryKey: ["pos", companyId, branchId],
    queryFn: () => posApi.list(companyId, branchId),
    enabled: !!branchId,
  });

  const { data: docTypes } = useQuery({
    queryKey: ["document-types"],
    queryFn: () => catalogApi.documentTypes(),
  });

  const { data: taxDefs } = useQuery({
    queryKey: ["tax-definitions"],
    queryFn: () => catalogApi.taxDefinitions(),
  });

  const getItemSearch = useCallback(
    (idx: number) => itemSearches[idx] ?? "",
    [itemSearches]
  );

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      invoiceApi.create({
        customerId: values.customerId,
        pointOfSaleId: values.pointOfSaleId,
        documentTypeCode: values.documentTypeCode,
        invoiceDate: new Date(values.invoiceDate).toISOString(),
        lines: values.lines.map((l) => ({
          billableItemId: l.billableItemId || undefined,
          itemName: l.itemName,
          itemDescription: l.itemDescription || undefined,
          quantity: parseFloat(l.quantity),
          unitPrice: parseFloat(l.unitPrice),
          discountAmount: l.discountAmount ? parseFloat(l.discountAmount) : undefined,
          taxCode: l.taxCode || undefined,
        })),
      }),
    onSuccess,
  });

  const selectedCustomerId = form.watch("customerId");
  const allCustomers = customers?.content ?? [];

  const computedTotals = form.watch("lines").reduce(
    (acc, l) => {
      const qty = parseFloat(l.quantity) || 0;
      const price = parseFloat(l.unitPrice) || 0;
      const disc = parseFloat(l.discountAmount ?? "") || 0;
      const sub = qty * price - disc;
      const taxDef = taxDefs?.find((t) => t.code === l.taxCode);
      const tax = taxDef ? sub * taxDef.rate : 0;
      return {
        subtotal: acc.subtotal + sub,
        tax: acc.tax + tax,
        total: acc.total + sub + tax,
      };
    },
    { subtotal: 0, tax: 0, total: 0 }
  );

  return (
    <form
      onSubmit={form.handleSubmit((v) => mutation.mutateAsync(v))}
      className="space-y-6"
    >
      {/* Header */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* Customer */}
        <div className="sm:col-span-2">
          <label className="text-sm font-medium mb-1 block">Cliente *</label>
          <Input
            placeholder="Buscar cliente por nombre..."
            value={customerSearch}
            onChange={(e) => setCustomerSearch(e.target.value)}
            className="mb-1"
          />
          {allCustomers.length > 0 && !selectedCustomerId && (
            <div className="rounded-md border bg-popover shadow-md max-h-48 overflow-y-auto">
              {allCustomers.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  className="w-full text-left px-3 py-2 text-sm hover:bg-muted"
                  onClick={() => {
                    form.setValue("customerId", c.id, { shouldValidate: true });
                    setCustomerSearch(c.legalName);
                  }}
                >
                  <span className="font-medium">{c.legalName}</span>
                  <span className="ml-2 text-xs text-muted-foreground">
                    {c.customerNumber}
                  </span>
                </button>
              ))}
            </div>
          )}
          {selectedCustomerId && (
            <button
              type="button"
              className="text-xs text-muted-foreground hover:underline"
              onClick={() => {
                form.setValue("customerId", "");
                setCustomerSearch("");
              }}
            >
              Cambiar cliente
            </button>
          )}
          {form.formState.errors.customerId && (
            <p className="text-xs text-destructive mt-1">
              {form.formState.errors.customerId.message}
            </p>
          )}
        </div>

        {/* Branch */}
        <div>
          <label className="text-sm font-medium mb-1 block">Sucursal *</label>
          <Controller
            control={form.control}
            name="branchId"
            render={({ field }) => (
              <Select
                value={field.value}
                onValueChange={(v) => {
                  field.onChange(v);
                  form.setValue("pointOfSaleId", "");
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Seleccionar..." />
                </SelectTrigger>
                <SelectContent>
                  {branches?.filter((b) => b.active).map((b) => (
                    <SelectItem key={b.id} value={b.id}>
                      {b.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          {form.formState.errors.branchId && (
            <p className="text-xs text-destructive mt-1">
              {form.formState.errors.branchId.message}
            </p>
          )}
        </div>

        {/* POS */}
        <div>
          <label className="text-sm font-medium mb-1 block">Punto de venta *</label>
          <Controller
            control={form.control}
            name="pointOfSaleId"
            render={({ field }) => (
              <Select
                value={field.value}
                onValueChange={field.onChange}
                disabled={!branchId}
              >
                <SelectTrigger>
                  <SelectValue placeholder={branchId ? "Seleccionar..." : "Seleccione sucursal"} />
                </SelectTrigger>
                <SelectContent>
                  {posList?.filter((p) => p.active).map((p) => (
                    <SelectItem key={p.id} value={p.id}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          {form.formState.errors.pointOfSaleId && (
            <p className="text-xs text-destructive mt-1">
              {form.formState.errors.pointOfSaleId.message}
            </p>
          )}
        </div>

        {/* Document type */}
        <div>
          <label className="text-sm font-medium mb-1 block">Tipo de documento *</label>
          <Controller
            control={form.control}
            name="documentTypeCode"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {docTypes?.map((d) => (
                    <SelectItem key={d.code} value={d.code}>
                      {d.code} – {d.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </div>

        {/* Invoice date */}
        <div>
          <label className="text-sm font-medium mb-1 block">Fecha de emisión *</label>
          <Input
            type="datetime-local"
            {...form.register("invoiceDate")}
          />
        </div>
      </div>

      <Separator />

      {/* Lines */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <p className="text-sm font-semibold">Líneas de factura</p>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => append(emptyLine())}
          >
            + Agregar línea
          </Button>
        </div>

        <div className="space-y-4">
          {fields.map((field, idx) => (
            <LineEditor
              key={field.id}
              idx={idx}
              form={form}
              taxDefs={taxDefs ?? []}
              itemSearch={getItemSearch(idx)}
              onItemSearchChange={(val) =>
                setItemSearches((prev) => ({ ...prev, [idx]: val }))
              }
              onRemove={fields.length > 1 ? () => remove(idx) : undefined}
            />
          ))}
        </div>
      </div>

      <Separator />

      {/* Totals */}
      <div className="rounded-lg bg-muted/40 p-4 text-sm space-y-1">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Subtotal</span>
          <span>${computedTotals.subtotal.toFixed(2)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Impuestos</span>
          <span>${computedTotals.tax.toFixed(2)}</span>
        </div>
        <div className="flex justify-between font-semibold text-base border-t pt-1 mt-1">
          <span>Total</span>
          <span>${computedTotals.total.toFixed(2)}</span>
        </div>
      </div>

      {mutation.isError && (
        <p className="text-sm text-destructive">
          {(mutation.error as { response?: { data?: { detail?: string } } })
            ?.response?.data?.detail ?? "Error al crear la factura."}
        </p>
      )}

      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancelar
          </Button>
        )}
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Creando..." : "Crear factura"}
        </Button>
      </div>
    </form>
  );
}

// ── Line Editor ──────────────────────────────────────────────────

interface LineEditorProps {
  idx: number;
  form: ReturnType<typeof useForm<FormValues>>;
  taxDefs: { code: string; name: string; rate: number }[];
  itemSearch: string;
  onItemSearchChange: (val: string) => void;
  onRemove?: () => void;
}

function LineEditor({
  idx,
  form,
  taxDefs,
  itemSearch,
  onItemSearchChange,
  onRemove,
}: LineEditorProps) {
  const debouncedSearch = useDebounce(itemSearch, 300);
  const [showResults, setShowResults] = useState(false);

  const { data: items } = useQuery({
    queryKey: ["items-search", debouncedSearch],
    queryFn: () => itemApi.list(debouncedSearch, "", 0, 8),
    enabled: debouncedSearch.length > 0,
  });

  const reg = (name: keyof FormValues["lines"][0]) =>
    form.register(`lines.${idx}.${name}` as `lines.${number}.${typeof name}`);

  const err = form.formState.errors.lines?.[idx];

  return (
    <div className="rounded-lg border p-4 relative">
      {onRemove && (
        <button
          type="button"
          onClick={onRemove}
          className="absolute top-3 right-3 text-muted-foreground hover:text-destructive text-xs"
        >
          Eliminar
        </button>
      )}
      <p className="text-xs font-semibold text-muted-foreground mb-3 uppercase tracking-wide">
        Línea {idx + 1}
      </p>

      {/* Item search */}
      <div className="mb-3 relative">
        <label className="text-xs font-medium mb-1 block">Descripción *</label>
        <div className="flex gap-2">
          <div className="flex-1 relative">
            <Input
              placeholder="Buscar ítem o escribir descripción..."
              value={itemSearch || form.watch(`lines.${idx}.itemName`)}
              onChange={(e) => {
                onItemSearchChange(e.target.value);
                form.setValue(`lines.${idx}.itemName`, e.target.value, {
                  shouldValidate: true,
                });
                setShowResults(true);
              }}
              onFocus={() => setShowResults(true)}
              onBlur={() => setTimeout(() => setShowResults(false), 200)}
            />
            {showResults && items && items.content.length > 0 && (
              <div className="absolute z-10 top-full mt-1 w-full rounded-md border bg-popover shadow-md max-h-48 overflow-y-auto">
                {items.content.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className="w-full text-left px-3 py-2 text-sm hover:bg-muted"
                    onClick={() => {
                      form.setValue(`lines.${idx}.billableItemId`, item.id);
                      form.setValue(`lines.${idx}.itemName`, item.name, {
                        shouldValidate: true,
                      });
                      form.setValue(
                        `lines.${idx}.itemDescription`,
                        item.description ?? ""
                      );
                      if (item.currentPrice) {
                        form.setValue(
                          `lines.${idx}.unitPrice`,
                          String(item.currentPrice.unitPrice),
                          { shouldValidate: true }
                        );
                      }
                      onItemSearchChange("");
                      setShowResults(false);
                    }}
                  >
                    <span className="font-medium">{item.name}</span>
                    {item.currentPrice && (
                      <span className="ml-2 text-xs text-muted-foreground">
                        ${Number(item.currentPrice.unitPrice).toFixed(2)}
                      </span>
                    )}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
        {err?.itemName && (
          <p className="text-xs text-destructive mt-1">{err.itemName.message}</p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div>
          <label className="text-xs font-medium mb-1 block">Cantidad *</label>
          <Input
            type="number"
            min="0.0001"
            step="0.0001"
            placeholder="1"
            {...reg("quantity")}
          />
          {err?.quantity && (
            <p className="text-xs text-destructive mt-1">{err.quantity.message}</p>
          )}
        </div>

        <div>
          <label className="text-xs font-medium mb-1 block">Precio unit. *</label>
          <Input
            type="number"
            min="0"
            step="0.01"
            placeholder="0.00"
            {...reg("unitPrice")}
          />
          {err?.unitPrice && (
            <p className="text-xs text-destructive mt-1">{err.unitPrice.message}</p>
          )}
        </div>

        <div>
          <label className="text-xs font-medium mb-1 block">Descuento</label>
          <Input
            type="number"
            min="0"
            step="0.01"
            placeholder="0.00"
            {...reg("discountAmount")}
          />
        </div>

        <div>
          <label className="text-xs font-medium mb-1 block">Impuesto</label>
          <Controller
            control={form.control}
            name={`lines.${idx}.taxCode`}
            render={({ field }) => (
              <Select value={field.value ?? ""} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Sin impuesto" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">Sin impuesto</SelectItem>
                  {taxDefs.map((t) => (
                    <SelectItem key={t.code} value={t.code}>
                      {t.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </div>
      </div>
    </div>
  );
}
