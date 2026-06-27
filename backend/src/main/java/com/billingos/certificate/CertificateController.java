package com.billingos.certificate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/companies/{companyId}/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public List<CertificateDto.Response> listCertificates(@PathVariable String companyId) {
        return certificateService.listCertificates(companyId);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<CertificateDto.Response> uploadCertificate(
            @PathVariable String companyId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("alias") String alias,
            @RequestPart(value = "password", required = false) String password) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateService.uploadCertificate(companyId, file, alias, password));
    }

    @DeleteMapping("/{certId}")
    public ResponseEntity<Void> deactivateCertificate(
            @PathVariable String companyId,
            @PathVariable String certId) {
        certificateService.deactivateCertificate(companyId, certId);
        return ResponseEntity.noContent().build();
    }
}
