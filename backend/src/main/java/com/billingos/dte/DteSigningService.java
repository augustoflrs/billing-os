package com.billingos.dte;

import com.billingos.certificate.Certificate;
import com.billingos.certificate.CertificateRepository;
import com.billingos.certificate.StorageService;
import com.billingos.common.status.StatusMachineService;
import com.billingos.company.CompanyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DteSigningService {

    private static final String ENTITY_TYPE   = "DTE";
    private static final String STATUS_DRAFT  = "dte_draft";
    private static final String STATUS_QUEUED = "dte_queued";
    private static final String STATUS_BLOCKED= "dte_blocked";
    private static final String SIGN_ALGORITHM= "SHA512withRSA";

    private final InvoiceDteRepository  dteRepository;
    private final CertificateRepository certificateRepository;
    private final StorageService        storageService;
    private final CompanyRepository     companyRepository;
    private final StatusMachineService  statusMachine;
    private final ObjectMapper          objectMapper;
    private final com.billingos.config.AppProperties appProperties;

    /**
     * Signs the DTE JSON payload and transitions the InvoiceDte record to dte_queued.
     * If no active, unexpired certificate is available, transitions to dte_blocked instead.
     *
     * The firma field (RSA-SHA512 Base64 signature of the canonical dteJson) is added
     * directly to the request_payload map so T-14 can transmit the complete signed document.
     */
    @Transactional
    public void sign(InvoiceDte dte) {
        if (!STATUS_DRAFT.equals(dte.getCurrentStatusId())) {
            log.debug("DTE {} is not in dte_draft ({}), skipping", dte.getId(), dte.getCurrentStatusId());
            return;
        }

        String companyId = companyRepository.findAll().stream()
                .findFirst()
                .map(c -> c.getId())
                .orElseThrow(() -> new IllegalStateException("No company configured"));

        Certificate cert = certificateRepository
                .findFirstByCompanyIdAndActiveTrueOrderByValidToDesc(companyId)
                .filter(c -> c.getValidTo().isAfter(OffsetDateTime.now()))
                .orElse(null);

        if (cert == null) {
            log.warn("No active DTE certificate for company {}; blocking DTE {}", companyId, dte.getId());
            statusMachine.transition(ENTITY_TYPE, dte.getId(), STATUS_DRAFT, STATUS_BLOCKED,
                    "No active certificate available");
            dte.setCurrentStatusId(STATUS_BLOCKED);
            dteRepository.save(dte);
            return;
        }

        try {
            String firma = computeFirma(dte.getRequestPayload(), cert);

            // Add firma into the payload map
            Map<String, Object> signed = new LinkedHashMap<>(dte.getRequestPayload());
            signed.put("firma", firma);
            dte.setRequestPayload(signed);

            statusMachine.transition(ENTITY_TYPE, dte.getId(), STATUS_DRAFT, STATUS_QUEUED,
                    "Signed with certificate " + cert.getId());
            dte.setCurrentStatusId(STATUS_QUEUED);
            dteRepository.save(dte);

            log.info("Signed DTE {} → dte_queued (cert={})", dte.getId(), cert.getId());

        } catch (CertificateNotAvailableException e) {
            log.error("Certificate load failed for DTE {}: {}", dte.getId(), e.getMessage());
            statusMachine.transition(ENTITY_TYPE, dte.getId(), STATUS_DRAFT, STATUS_BLOCKED, e.getMessage());
            dte.setCurrentStatusId(STATUS_BLOCKED);
            dteRepository.save(dte);
        } catch (Exception e) {
            log.error("Signing failed for DTE {}: {}", dte.getId(), e.getMessage(), e);
            throw new RuntimeException("DTE signing failed: " + e.getMessage(), e);
        }
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private String computeFirma(Map<String, Object> dteJson, Certificate cert) {
        String password = appProperties.getDte().getCertificatePassword();
        PrivateKey privateKey = loadPrivateKey(cert, password);

        String canonical = toCanonicalJson(dteJson);
        try {
            Signature sig = Signature.getInstance(SIGN_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("RSA signing failed: " + e.getMessage(), e);
        }
    }

    private PrivateKey loadPrivateKey(Certificate cert, String password) {
        try (InputStream is = storageService.retrieve(cert.getCertificatePath())) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            char[] pwd = password != null ? password.toCharArray() : new char[0];
            ks.load(is, pwd);

            String alias = cert.getAlias();
            if (!ks.containsAlias(alias)) {
                // Fall back to first key entry
                java.util.Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String a = aliases.nextElement();
                    if (ks.isKeyEntry(a)) { alias = a; break; }
                }
            }

            java.security.Key key = ks.getKey(alias, pwd);
            if (!(key instanceof PrivateKey)) {
                throw new CertificateNotAvailableException(
                        "No private key found for alias '" + alias + "' in certificate " + cert.getId());
            }
            return (PrivateKey) key;

        } catch (CertificateNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateNotAvailableException(
                    "Cannot load PKCS12 from MinIO path " + cert.getCertificatePath() + ": " + e.getMessage());
        }
    }

    private String toCanonicalJson(Map<String, Object> map) {
        try {
            // Remove any pre-existing firma before computing the signature input
            Map<String, Object> withoutFirma = new LinkedHashMap<>(map);
            withoutFirma.remove("firma");
            return objectMapper.writeValueAsString(withoutFirma);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize DTE JSON for signing", e);
        }
    }
}
