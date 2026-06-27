package com.billingos.certificate;

import com.billingos.common.UlidGenerator;
import com.billingos.common.exception.ResourceNotFoundException;
import com.billingos.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.KeyStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CompanyRepository companyRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<CertificateDto.Response> listCertificates(String companyId) {
        requireCompany(companyId);
        return certificateRepository.findByCompanyIdOrderByValidToDesc(companyId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CertificateDto.Response uploadCertificate(
            String companyId, MultipartFile file, String alias, String password) {
        requireCompany(companyId);

        // Parse keystore to extract validity dates
        KeystoreInfo info = readKeystore(file, password);

        String id = UlidGenerator.generate();
        String objectKey = "certificates/" + companyId + "/" + id;

        try {
            storageService.store(objectKey, file.getInputStream(),
                    file.getSize(), "application/octet-stream");
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }

        Certificate cert = new Certificate();
        cert.setId(id);
        cert.setCompanyId(companyId);
        cert.setAlias(alias);
        cert.setCertificatePath(objectKey);
        cert.setValidFrom(info.validFrom());
        cert.setValidTo(info.validTo());

        return toResponse(certificateRepository.save(cert));
    }

    @Transactional
    public void deactivateCertificate(String companyId, String certId) {
        Certificate cert = certificateRepository.findById(certId)
                .filter(c -> c.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certId));
        cert.setActive(false);
        certificateRepository.save(cert);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void requireCompany(String companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found: " + companyId);
        }
    }

    private KeystoreInfo readKeystore(MultipartFile file, String password) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String type = filename.endsWith(".p12") || filename.endsWith(".pfx") ? "PKCS12" : "JKS";

        try {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(file.getInputStream(), password != null ? password.toCharArray() : null);

            Enumeration<String> aliases = ks.aliases();
            Date notAfter = null;
            Date notBefore = null;

            while (aliases.hasMoreElements()) {
                String a = aliases.nextElement();
                if (ks.isKeyEntry(a)) {
                    java.security.cert.X509Certificate x509 =
                            (java.security.cert.X509Certificate) ks.getCertificate(a);
                    notBefore = x509.getNotBefore();
                    notAfter = x509.getNotAfter();
                    break;
                }
            }

            if (notBefore == null || notAfter == null) {
                // Fallback: use first certificate entry
                aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String a = aliases.nextElement();
                    java.security.cert.X509Certificate x509 =
                            (java.security.cert.X509Certificate) ks.getCertificate(a);
                    if (x509 != null) {
                        notBefore = x509.getNotBefore();
                        notAfter = x509.getNotAfter();
                        break;
                    }
                }
            }

            if (notBefore == null) {
                throw new IllegalArgumentException("No valid certificate found in keystore");
            }

            return new KeystoreInfo(
                    notBefore.toInstant().atOffset(ZoneOffset.UTC),
                    notAfter.toInstant().atOffset(ZoneOffset.UTC)
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot read keystore — check file format and password: " + e.getMessage());
        }
    }

    private CertificateDto.Response toResponse(Certificate c) {
        return new CertificateDto.Response(
                c.getId(), c.getCompanyId(), c.getAlias(),
                c.getCertificatePath(), c.getValidFrom(), c.getValidTo(), c.isActive()
        );
    }

    private record KeystoreInfo(OffsetDateTime validFrom, OffsetDateTime validTo) {}
}
