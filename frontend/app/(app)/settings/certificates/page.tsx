"use client";

import { useRef, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { companyApi } from "@/lib/api/company";
import { certificateApi, CertificateResponse } from "@/lib/api/certificate";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";

function CertCard({
  cert,
  onDeactivate,
}: {
  cert: CertificateResponse;
  onDeactivate: () => void;
}) {
  const now = new Date();
  const validTo = new Date(cert.validTo);
  const expired = validTo < now;
  const expiresSoon =
    !expired && validTo.getTime() - now.getTime() < 30 * 24 * 60 * 60 * 1000;

  return (
    <div className={`rounded-lg border p-4 ${!cert.active ? "opacity-50" : ""}`}>
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <p className="font-medium">{cert.alias}</p>
            {expired && (
              <span className="rounded-full bg-destructive/10 px-2 py-0.5 text-xs text-destructive">
                Expirado
              </span>
            )}
            {expiresSoon && (
              <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-xs text-yellow-800">
                Vence pronto
              </span>
            )}
            {!cert.active && (
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                Inactivo
              </span>
            )}
          </div>
          <p className="text-sm text-muted-foreground">
            Válido: {new Date(cert.validFrom).toLocaleDateString("es-SV")} –{" "}
            {validTo.toLocaleDateString("es-SV")}
          </p>
        </div>
        {cert.active && (
          <Button
            variant="ghost"
            size="sm"
            className="text-destructive"
            onClick={() => {
              if (confirm("¿Desactivar este certificado?")) onDeactivate();
            }}
          >
            Desactivar
          </Button>
        )}
      </div>
    </div>
  );
}

function UploadForm({
  companyId,
  onSuccess,
  onCancel,
}: {
  companyId: string;
  onSuccess: () => void;
  onCancel: () => void;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [alias, setAlias] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);
  const qc = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => certificateApi.upload(companyId, file!, alias, password || undefined),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["certificates", companyId] });
      onSuccess();
    },
    onError: (e: { response?: { data?: { detail?: string } } }) => {
      setError(e?.response?.data?.detail ?? "Error al subir el certificado.");
    },
  });

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">
          Archivo (.p12, .pfx, .jks) *
        </label>
        <input
          ref={fileRef}
          type="file"
          accept=".p12,.pfx,.jks"
          className="block w-full text-sm file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Alias *</label>
        <Input
          placeholder="Certificado MH 2025"
          value={alias}
          onChange={(e) => setAlias(e.target.value)}
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Contraseña del keystore</label>
        <Input
          type="password"
          placeholder="Dejar en blanco si no aplica"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={onCancel}>Cancelar</Button>
        <Button
          disabled={!file || !alias || mutation.isPending}
          onClick={() => { setError(""); mutation.mutate(); }}
        >
          {mutation.isPending ? "Subiendo..." : "Subir certificado"}
        </Button>
      </div>
    </div>
  );
}

export default function CertificatesPage() {
  const qc = useQueryClient();
  const [showUpload, setShowUpload] = useState(false);

  const { data: company, isLoading: loadingCompany } = useQuery({
    queryKey: ["company"],
    queryFn: companyApi.get,
    retry: false,
  });

  const { data: certs = [], isLoading: loadingCerts } = useQuery({
    queryKey: ["certificates", company?.id],
    queryFn: () => certificateApi.list(company!.id),
    enabled: !!company,
  });

  const deactivate = useMutation({
    mutationFn: (certId: string) => certificateApi.deactivate(company!.id, certId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["certificates", company?.id] }),
  });

  if (loadingCompany || loadingCerts) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 p-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  if (!company) {
    return (
      <div className="p-8 text-muted-foreground">
        Configure su empresa antes de subir certificados.
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Certificados DTE</h1>
          <p className="mt-1 text-muted-foreground">
            Certificados digitales para firma de documentos electrónicos.
          </p>
        </div>
        {!showUpload && (
          <Button onClick={() => setShowUpload(true)}>+ Subir certificado</Button>
        )}
      </div>

      {showUpload && (
        <div className="mb-6 rounded-lg border bg-card p-6">
          <h2 className="mb-4 font-semibold">Nuevo certificado</h2>
          <UploadForm
            companyId={company.id}
            onSuccess={() => setShowUpload(false)}
            onCancel={() => setShowUpload(false)}
          />
        </div>
      )}

      <div className="space-y-3">
        {certs.length === 0 && !showUpload && (
          <p className="text-muted-foreground">
            No hay certificados. Suba uno para habilitar la emisión de DTE.
          </p>
        )}
        {certs.map((cert) => (
          <CertCard
            key={cert.id}
            cert={cert}
            onDeactivate={() => deactivate.mutate(cert.id)}
          />
        ))}
      </div>
    </div>
  );
}
