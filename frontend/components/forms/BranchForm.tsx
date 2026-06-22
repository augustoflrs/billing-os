"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation } from "@tanstack/react-query";
import { branchApi, BranchResponse } from "@/lib/api/branch";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { apiClient } from "@/lib/api-client";

interface DepartmentOption { code: string; name: string; }
interface MunicipalityOption { code: string; name: string; }

const schema = z.object({
  code: z.string().min(1, "Requerido").max(20),
  name: z.string().min(1, "Requerido").max(255),
  addressLine1: z.string().min(1, "Requerido").max(255),
  departmentCode: z.string().optional().or(z.literal("")),
  municipalityCode: z.string().optional().or(z.literal("")),
  phone: z.string().max(50).optional().or(z.literal("")),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  companyId: string;
  existing?: BranchResponse;
  onSuccess: (branch: BranchResponse) => void;
  onCancel: () => void;
}

export function BranchForm({ companyId, existing, onSuccess, onCancel }: Props) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      code: existing?.code ?? "",
      name: existing?.name ?? "",
      addressLine1: existing?.addressLine1 ?? "",
      departmentCode: existing?.departmentCode ?? "",
      municipalityCode: existing?.municipalityCode ?? "",
      phone: existing?.phone ?? "",
    },
  });

  const selectedDept = form.watch("departmentCode");

  const { data: departments = [] } = useQuery<DepartmentOption[]>({
    queryKey: ["departments"],
    queryFn: () => apiClient.get("/catalogs/departments").then((r) => r.data),
    staleTime: Infinity,
  });

  const { data: municipalities = [] } = useQuery<MunicipalityOption[]>({
    queryKey: ["municipalities", selectedDept],
    queryFn: () =>
      apiClient
        .get(`/catalogs/departments/${selectedDept}/municipalities`)
        .then((r) => r.data),
    enabled: !!selectedDept,
    staleTime: Infinity,
  });

  // Clear municipality when department changes
  useEffect(() => {
    form.setValue("municipalityCode", "");
  }, [selectedDept, form]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = {
        ...values,
        departmentCode: values.departmentCode || undefined,
        municipalityCode: values.municipalityCode || undefined,
        phone: values.phone || undefined,
      };
      return existing
        ? branchApi.update(companyId, existing.id, payload)
        : branchApi.create(companyId, payload);
    },
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
                <FormControl><Input placeholder="SV-01" {...field} /></FormControl>
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
                <FormControl><Input placeholder="Casa matriz" {...field} /></FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="addressLine1"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Dirección *</FormLabel>
              <FormControl><Input placeholder="Calle, colonia, municipio" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="departmentCode"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Departamento</FormLabel>
                <Select onValueChange={field.onChange} value={field.value ?? ""}>
                  <FormControl>
                    <SelectTrigger><SelectValue placeholder="Seleccionar..." /></SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="">Sin departamento</SelectItem>
                    {departments.map((d) => (
                      <SelectItem key={d.code} value={d.code}>{d.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="municipalityCode"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Municipio</FormLabel>
                <Select
                  onValueChange={field.onChange}
                  value={field.value ?? ""}
                  disabled={!selectedDept}
                >
                  <FormControl>
                    <SelectTrigger><SelectValue placeholder="Seleccionar..." /></SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="">Sin municipio</SelectItem>
                    {municipalities.map((m) => (
                      <SelectItem key={m.code} value={m.code}>{m.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <FormField
          control={form.control}
          name="phone"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Teléfono</FormLabel>
              <FormControl><Input placeholder="2222-2222" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {mutation.isError && (
          <p className="text-sm text-destructive">
            {(mutation.error as { response?: { data?: { detail?: string } } })
              ?.response?.data?.detail ?? "Error al guardar."}
          </p>
        )}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando..." : existing ? "Guardar" : "Crear"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
