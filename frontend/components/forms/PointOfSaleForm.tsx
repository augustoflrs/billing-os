"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { posApi, PointOfSaleResponse } from "@/lib/api/branch";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

const schema = z.object({
  code: z.string().min(1, "Requerido").max(20),
  name: z.string().min(1, "Requerido").max(255),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  companyId: string;
  branchId: string;
  existing?: PointOfSaleResponse;
  onSuccess: () => void;
  onCancel: () => void;
}

export function PointOfSaleForm({ companyId, branchId, existing, onSuccess, onCancel }: Props) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      code: existing?.code ?? "",
      name: existing?.name ?? "",
    },
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      existing
        ? posApi.update(companyId, branchId, existing.id, values)
        : posApi.create(companyId, branchId, values),
    onSuccess,
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((v) => mutation.mutateAsync(v))} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="code"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Código *</FormLabel>
                <FormControl><Input placeholder="PDV-01" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Nombre *</FormLabel>
                <FormControl><Input placeholder="Caja 1" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {mutation.isError && (
          <p className="text-sm text-destructive">
            {(mutation.error as { response?: { data?: { detail?: string } } })
              ?.response?.data?.detail ?? "Error al guardar."}
          </p>
        )}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando..." : existing ? "Guardar" : "Agregar"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
