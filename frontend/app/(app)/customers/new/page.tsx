"use client";

import { useRouter } from "next/navigation";
import { CustomerForm } from "@/components/forms/CustomerForm";
import { CustomerResponse } from "@/lib/api/customer";

export default function NewCustomerPage() {
  const router = useRouter();

  return (
    <div className="mx-auto max-w-2xl p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold">Nuevo cliente</h1>
        <p className="mt-1 text-muted-foreground">
          Complete los datos básicos. El perfil fiscal es opcional y solo necesario para DTE.
        </p>
      </div>
      <div className="rounded-lg border bg-card p-8 shadow-sm">
        <CustomerForm
          onSuccess={(c: CustomerResponse) => router.push(`/customers/${c.id}`)}
          onCancel={() => router.push("/customers")}
        />
      </div>
    </div>
  );
}
