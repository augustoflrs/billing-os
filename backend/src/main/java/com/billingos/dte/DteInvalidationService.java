package com.billingos.dte;

import com.billingos.common.status.StatusMachineService;
import com.billingos.company.Company;
import com.billingos.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds, signs, and submits a DTE invalidation (anulación) to MH.
 *
 * Called when an invoice with an accepted DTE is cancelled. MH rejection or network
 * failure is treated as non-fatal — the invoice is cancelled regardless, but the
 * outcome is written to the DTE event log and logged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DteInvalidationService {

    private static final String ENTITY_TYPE    = "DTE";
    private static final String S_ACCEPTED     = "dte_accepted";
    private static final String S_INVALIDATED  = "dte_invalidated";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ZoneId SV_ZONE = ZoneId.of("America/El_Salvador");

    private final InvoiceDteRepository      dteRepository;
    private final InvoiceDteEventRepository eventRepository;
    private final DteSigningService         signingService;
    private final MhApiClient               mhApiClient;
    private final CompanyRepository         companyRepository;
    private final StatusMachineService      statusMachine;

    /**
     * Submits an anulación to MH for the given invoice's DTE (if it is in dte_accepted).
     * Does nothing if no accepted DTE exists.
     *
     * @return true if MH accepted the invalidation, false otherwise
     */
    @Transactional
    public boolean invalidate(String invoiceId, String reason) {
        InvoiceDte dte = dteRepository.findByInvoiceId(invoiceId).orElse(null);
        if (dte == null || !S_ACCEPTED.equals(dte.getCurrentStatusId())) {
            return true; // nothing to invalidate — treat as success
        }

        Company company = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No company configured"));

        Map<String, Object> anulacionPayload = buildAnulacionPayload(dte, company, reason);

        Map<String, Object> signedPayload;
        try {
            signedPayload = signingService.signPayload(anulacionPayload);
        } catch (CertificateNotAvailableException e) {
            log.error("Cannot sign invalidation for invoice {}: {}", invoiceId, e.getMessage());
            writeEvent(dte.getId(), "INVALIDATION_ERROR", anulacionPayload, null);
            return false;
        }

        MhResponse response = mhApiClient.submitInvalidation(signedPayload, company.getNit());

        switch (response.kind()) {
            case ACCEPTED -> {
                statusMachine.transition(ENTITY_TYPE, dte.getId(), S_ACCEPTED, S_INVALIDATED,
                        "MH accepted invalidation: " + response.mhCode());
                dte.setCurrentStatusId(S_INVALIDATED);
                dteRepository.save(dte);
                writeEvent(dte.getId(), "INVALIDATION_ACCEPTED", response.requestJson(), response.responseJson());
                log.info("DTE {} invalidated by MH — code={}", dte.getId(), response.mhCode());
                return true;
            }
            case REJECTED -> {
                log.warn("MH rejected invalidation for DTE {}: {} {}", dte.getId(), response.mhCode(), response.mhMessage());
                writeEvent(dte.getId(), "INVALIDATION_ERROR", response.requestJson(), response.responseJson());
                return false;
            }
            case TRANSIENT_ERROR -> {
                log.error("MH unreachable during invalidation for DTE {}: {}", dte.getId(), response.mhMessage());
                writeEvent(dte.getId(), "INVALIDATION_ERROR", response.requestJson(), null);
                return false;
            }
        }
        return false;
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private Map<String, Object> buildAnulacionPayload(InvoiceDte dte, Company company, String reason) {
        var now = Instant.now().atZone(SV_ZONE).toLocalDateTime();

        // Retrieve selloRecibido from response_payload
        String selloRecibido = "";
        if (dte.getResponsePayload() != null) {
            Object sello = dte.getResponsePayload().get("selloRecibido");
            if (sello != null) selloRecibido = sello.toString();
        }

        // Get tipoDte from request_payload.identificacion.tipoDte
        String tipoDteDoc = "01";
        if (dte.getRequestPayload() != null) {
            Object identObj = dte.getRequestPayload().get("identificacion");
            if (identObj instanceof Map<?,?> ident) {
                Object tipo = ident.get("tipoDte");
                if (tipo != null) tipoDteDoc = tipo.toString();
            }
        }

        // fecEmi from the original DTE
        String fecEmiOrig = "";
        if (dte.getRequestPayload() != null) {
            Object identObj = dte.getRequestPayload().get("identificacion");
            if (identObj instanceof Map<?,?> ident) {
                Object fec = ident.get("fecEmi");
                if (fec != null) fecEmiOrig = fec.toString();
            }
        }

        Map<String, Object> identificacion = new LinkedHashMap<>();
        identificacion.put("version",          2);
        identificacion.put("ambiente",         "00");
        identificacion.put("codigoGeneracion", UUID.randomUUID().toString().toUpperCase());
        identificacion.put("fecAnula",         now.format(DATE_FMT));
        identificacion.put("horAnula",         now.format(TIME_FMT));

        Map<String, Object> emisor = new LinkedHashMap<>();
        emisor.put("nit",      company.getNit());
        emisor.put("nrc",      company.getNrc() != null ? company.getNrc() : "");
        emisor.put("nombre",   company.getLegalName());
        emisor.put("correo",   company.getEmail() != null ? company.getEmail() : "");
        emisor.put("telefono", company.getPhone() != null ? company.getPhone() : "");

        Map<String, Object> documento = new LinkedHashMap<>();
        documento.put("tipoDteDoc",          tipoDteDoc);
        documento.put("codigoGeneracion",    dte.getGenerationCode());
        documento.put("selloRecibido",       selloRecibido);
        documento.put("numeroControl",       dte.getControlNumber());
        documento.put("fecEmi",              fecEmiOrig);
        documento.put("montoIva",            "0.00");
        documento.put("codigoGeneracionR",   null);
        documento.put("nombreResponsable",   company.getLegalName());
        documento.put("tipDocResponsable",   "13");   // NIT
        documento.put("numDocResponsable",   company.getNit());
        documento.put("nombreSolicita",      company.getLegalName());
        documento.put("tipDocSolicita",      "13");
        documento.put("numDocSolicita",      company.getNit());
        documento.put("causalAnulacion",     reason != null && !reason.isBlank() ? reason : "Anulación de documento");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("identificacion", identificacion);
        root.put("emisor",         emisor);
        root.put("documento",      documento);
        return root;
    }

    private void writeEvent(String dteId, String eventTypeCode,
                            Map<String, Object> req, Map<String, Object> res) {
        InvoiceDteEvent event = new InvoiceDteEvent();
        event.setInvoiceDteId(dteId);
        event.setEventTypeCode(eventTypeCode);
        event.setRequestJson(req);
        event.setResponseJson(res);
        eventRepository.save(event);
    }
}
