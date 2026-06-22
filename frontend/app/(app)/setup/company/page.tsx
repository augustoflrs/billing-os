"use client";

import { useRouter } from "next/navigation";
import { CompanyForm } from "@/components/forms/CompanyForm";
import { CompanyResponse } from "@/lib/api/company";

export default function SetupCompanyPage() {
  const router = useRouter();

  function handleSuccess(_company: CompanyResponse) {
    router.push("/dashboard");
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="w-full max-w-2xl">
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-bold">Configura tu empresa</h1>
          <p className="mt-2 text-muted-foreground">
            Ingresa los datos de tu empresa para comenzar a emitir documentos.
          </p>
        </div>
        <div className="rounded-lg border bg-card p-8 shadow-sm">
          <CompanyForm onSuccess={handleSuccess} />
        </div>
      </div>
    </div>
  );
}
