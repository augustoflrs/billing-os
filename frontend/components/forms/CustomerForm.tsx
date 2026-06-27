"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation } from "@tanstack/react-query";
import { customerApi, CustomerResponse, CustomerRequest } from "@/lib/api/customer";
import { apiClient } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from "@/components/ui/form";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";

interface DeptOption { code: string; name: string; }
interface MuniOption { code: string; name: string; }

const DOCUMENT_TYPES = [
  { value: "DUI", label: "DUI" },
  { value: "NIT", label: "NIT" },
  { value: "PASAPORTE", label: "Pasaporte" },
  { value: "CARNET_RESIDENTE", label: "Carnet de residente" },
  { value: "OTRO", label: "Otro" },
];

const schema = z.object({
  legalName: z.string().min(1, "Requerido").max(255),
  tradeName: z.string().max(255).optional().or(z.literal("")),
  email: z.string().email("Email inválido").optional().or(z.literal("")),
  phone: z.string().max(50).optional().or(z.literal("")),
  hasAddress: z.boolean(),
  addressLine1: z.string().max(255).optional().or(z.literal("")),
  departmentCode: z.string().optional().or(z.literal("")),
  municipalityCode: z.string().optional().or(z.literal("")),
  hasTaxProfile: z.boolean(),
  documentType: z.string().optional().or(z.literal("")),
  documentNumber: z.string().max(50).optional().or(z.literal("")),
  nit: z.string().max(20).optional().or(z.literal("")),
  nrc: z.string().max(20).optional().or(z.literal("")),
  economicActivityCode: z.string().max(20).optional().or(z.literal("")),
}).superRefine((data, ctx) => {
  if (data.hasAddress && !data.addressLine1) {
    ctx.addIssue({ code: "custom", path: ["addressLine1"], message: "Requerido" });
  }
  if (data.hasTaxProfile) {
    if (!data.documentType) ctx.addIssue({ code: "custom", path: ["documentType"], message: "Requerido" });
    if (!data.documentNumber) ctx.addIssue({ code: "custom", path: ["documentNumber"], message: "Requerido" });
  }
});

type FormValues = z.infer<typeof schema>;

interface Props {
  existing?: CustomerResponse;
  onSuccess: (c: CustomerResponse) => void;
  onCancel?: () => void;
}

export function CustomerForm({ existing, onSuccess, onCancel }: Props) {
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      legalName: existing?.legalName ?? "",
      tradeName: existing?.tradeName ?? "",
      email: existing?.email ?? "",
      phone: existing?.phone ?? "",
      hasAddress: !!existing?.address,
      addressLine1: existing?.address?.addressLine1 ?? "",
      departmentCode: existing?.address?.departmentCode ?? "",
      municipalityCode: existing?.address?.municipalityCode ?? "",
      hasTaxProfile: !!existing?.taxProfile,
      documentType: existing?.taxProfile?.documentType ?? "",
      documentNumber: existing?.taxProfile?.documentNumber ?? "",
      nit: existing?.taxProfile?.nit ?? "",
      nrc: existing?.taxProfile?.nrc ?? "",
      economicActivityCode: existing?.taxProfile?.economicActivityCode ?? "",
    },
  });

  const hasAddress = form.watch("hasAddress");
  const hasTaxProfile = form.watch("hasTaxProfile");
  const selectedDept = form.watch("departmentCode");

  const { data: departments = [] } = useQuery<DeptOption[]>({
    queryKey: ["departments"],
    queryFn: () => apiClient.get("/catalogs/departments").then((r) => r.data),
    staleTime: Infinity,
  });

  const { data: municipalities = [] } = useQuery<MuniOption[]>({
    queryKey: ["municipalities", selectedDept],
    queryFn: () =>
      apiClient.get(`/catalogs/departments/${selectedDept}/municipalities`).then((r) => r.data),
    enabled: !!selectedDept,
    staleTime: Infinity,
  });

  useEffect(() => {
    form.setValue("municipalityCode", "");
  }, [selectedDept, form]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload: CustomerRequest = {
        legalName: values.legalName,
        tradeName: values.tradeName || undefined,
        email: values.email || undefined,
        phone: values.phone || undefined,
        address: values.hasAddress && values.addressLine1 ? {
          addressLine1: values.addressLine1,
          departmentCode: values.departmentCode || undefined,
          municipalityCode: values.municipalityCode || undefined,
        } : undefined,
        taxProfile: values.hasTaxProfile && values.documentType && values.documentNumber ? {
          documentType: values.documentType,
          documentNumber: values.documentNumber,
          nit: values.nit || undefined,
          nrc: values.nrc || undefined,
          economicActivityCode: values.economicActivityCode || undefined,
        } : undefined,
      };
      return existing
        ? customerApi.update(existing.id, existing.version, payload)
        : customerApi.create(payload);
    },
    onSuccess,
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((v) => mutation.mutateAsync(v))} className="space-y-6">
        {/* Basic info */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField control={form.control} name="legalName" render={({ field }) => (
            <FormItem>
              <FormLabel>Nombre / Razón social *</FormLabel>
              <FormControl><Input placeholder="Juan García / Empresa S.A." {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />
          <FormField control={form.control} name="tradeName" render={({ field }) => (
            <FormItem>
              <FormLabel>Nombre comercial</FormLabel>
              <FormControl><Input placeholder="Opcional" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />
          <FormField control={form.control} name="email" render={({ field }) => (
            <FormItem>
              <FormLabel>Correo electrónico</FormLabel>
              <FormControl><Input type="email" placeholder="cliente@email.com" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />
          <FormField control={form.control} name="phone" render={({ field }) => (
            <FormItem>
              <FormLabel>Teléfono</FormLabel>
              <FormControl><Input placeholder="7777-7777" {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />
        </div>

        {/* Address */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <input
              type="checkbox"
              id="hasAddress"
              checked={hasAddress}
              onChange={(e) => form.setValue("hasAddress", e.target.checked)}
              className="h-4 w-4"
            />
            <label htmlFor="hasAddress" className="text-sm font-medium">Agregar dirección</label>
          </div>
          {hasAddress && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3 pl-6">
              <FormField control={form.control} name="addressLine1" render={({ field }) => (
                <FormItem className="sm:col-span-3">
                  <FormLabel>Dirección *</FormLabel>
                  <FormControl><Input placeholder="Calle, colonia, número" {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="departmentCode" render={({ field }) => (
                <FormItem>
                  <FormLabel>Departamento</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value ?? ""}>
                    <FormControl><SelectTrigger><SelectValue placeholder="Seleccionar..." /></SelectTrigger></FormControl>
                    <SelectContent>
                      <SelectItem value="">—</SelectItem>
                      {departments.map((d) => <SelectItem key={d.code} value={d.code}>{d.name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="municipalityCode" render={({ field }) => (
                <FormItem>
                  <FormLabel>Municipio</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value ?? ""} disabled={!selectedDept}>
                    <FormControl><SelectTrigger><SelectValue placeholder="Seleccionar..." /></SelectTrigger></FormControl>
                    <SelectContent>
                      <SelectItem value="">—</SelectItem>
                      {municipalities.map((m) => <SelectItem key={m.code} value={m.code}>{m.name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )} />
            </div>
          )}
        </div>

        <Separator />

        {/* Tax profile */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <input
              type="checkbox"
              id="hasTaxProfile"
              checked={hasTaxProfile}
              onChange={(e) => form.setValue("hasTaxProfile", e.target.checked)}
              className="h-4 w-4"
            />
            <label htmlFor="hasTaxProfile" className="text-sm font-medium">Perfil fiscal (DTE)</label>
          </div>
          {hasTaxProfile && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 pl-6">
              <FormField control={form.control} name="documentType" render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo de documento *</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value ?? ""}>
                    <FormControl><SelectTrigger><SelectValue placeholder="Seleccionar..." /></SelectTrigger></FormControl>
                    <SelectContent>
                      {DOCUMENT_TYPES.map((d) => <SelectItem key={d.value} value={d.value}>{d.label}</SelectItem>)}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="documentNumber" render={({ field }) => (
                <FormItem>
                  <FormLabel>Número de documento *</FormLabel>
                  <FormControl><Input placeholder="00000000-0" {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="nit" render={({ field }) => (
                <FormItem>
                  <FormLabel>NIT</FormLabel>
                  <FormControl><Input placeholder="0000-000000-000-0" {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="nrc" render={({ field }) => (
                <FormItem>
                  <FormLabel>NRC</FormLabel>
                  <FormControl><Input placeholder="000000-0" {...field} /></FormControl>
                  <FormMessage />
                </FormItem>
              )} />
            </div>
          )}
        </div>

        {mutation.isError && (
          <p className="text-sm text-destructive">
            {(mutation.error as { response?: { data?: { detail?: string } } })
              ?.response?.data?.detail ?? "Error al guardar. Intente de nuevo."}
          </p>
        )}

        <div className="flex justify-end gap-2">
          {onCancel && <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>}
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Guardando..." : existing ? "Guardar cambios" : "Crear cliente"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
