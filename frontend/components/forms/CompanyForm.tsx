"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { catalogApi, companyApi, CompanyResponse } from "@/lib/api/company";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormDescription,
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

const NIT_REGEX = /^\d{4}-\d{6}-\d{3}-\d$/;

const schema = z.object({
  legalName: z.string().min(1, "Requerido").max(255),
  tradeName: z.string().max(255).optional().or(z.literal("")),
  nit: z
    .string()
    .min(1, "Requerido")
    .regex(NIT_REGEX, "Formato: 0000-000000-000-0"),
  nrc: z.string().max(20).optional().or(z.literal("")),
  economicActivityCode: z.string().optional().or(z.literal("")),
  email: z
    .string()
    .email("Email inválido")
    .optional()
    .or(z.literal("")),
  phone: z.string().max(50).optional().or(z.literal("")),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  existing?: CompanyResponse;
  onSuccess?: (company: CompanyResponse) => void;
}

export function CompanyForm({ existing, onSuccess }: Props) {
  const queryClient = useQueryClient();

  const { data: activities = [], isLoading: loadingActivities } = useQuery({
    queryKey: ["economic-activities"],
    queryFn: catalogApi.economicActivities,
    staleTime: Infinity,
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      legalName: existing?.legalName ?? "",
      tradeName: existing?.tradeName ?? "",
      nit: existing?.nit ?? "",
      nrc: existing?.nrc ?? "",
      economicActivityCode: existing?.economicActivityCode ?? "",
      email: existing?.email ?? "",
      phone: existing?.phone ?? "",
    },
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = {
        ...values,
        tradeName: values.tradeName || undefined,
        nrc: values.nrc || undefined,
        economicActivityCode: values.economicActivityCode || undefined,
        email: values.email || undefined,
        phone: values.phone || undefined,
      };
      return existing
        ? companyApi.update(existing.id, existing.version, payload)
        : companyApi.create(payload);
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["company"], data);
      onSuccess?.(data);
    },
  });

  async function onSubmit(values: FormValues) {
    await mutation.mutateAsync(values);
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* Legal name */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="legalName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Razón social *</FormLabel>
                <FormControl>
                  <Input placeholder="Empresa S.A. de C.V." {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="tradeName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Nombre comercial</FormLabel>
                <FormControl>
                  <Input placeholder="Nombre que aparece en facturas" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* NIT / NRC */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="nit"
            render={({ field }) => (
              <FormItem>
                <FormLabel>NIT *</FormLabel>
                <FormControl>
                  <Input placeholder="0000-000000-000-0" {...field} />
                </FormControl>
                <FormDescription>
                  Número de Identificación Tributaria
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="nrc"
            render={({ field }) => (
              <FormItem>
                <FormLabel>NRC</FormLabel>
                <FormControl>
                  <Input placeholder="000000-0" {...field} />
                </FormControl>
                <FormDescription>
                  Número de Registro de Contribuyente
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* Economic activity */}
        <FormField
          control={form.control}
          name="economicActivityCode"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Actividad económica</FormLabel>
              <Select
                onValueChange={field.onChange}
                value={field.value ?? ""}
                disabled={loadingActivities}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Seleccionar actividad..." />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="">Sin actividad</SelectItem>
                  {activities.map((a) => (
                    <SelectItem key={a.code} value={a.code}>
                      {a.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Contact */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Correo electrónico</FormLabel>
                <FormControl>
                  <Input
                    type="email"
                    placeholder="facturacion@empresa.com"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="phone"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Teléfono</FormLabel>
                <FormControl>
                  <Input placeholder="2222-2222" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {mutation.isError && (
          <p className="text-sm text-destructive">
            {(mutation.error as { response?: { data?: { detail?: string } } })
              ?.response?.data?.detail ?? "Error al guardar. Intente de nuevo."}
          </p>
        )}

        <div className="flex justify-end">
          <Button
            type="submit"
            disabled={form.formState.isSubmitting || mutation.isPending}
          >
            {mutation.isPending
              ? "Guardando..."
              : existing
              ? "Guardar cambios"
              : "Crear empresa"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
