package com.billingos.invoice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/invoices/{invoiceId}/pdf")
@RequiredArgsConstructor
public class InvoicePdfController {

    private final InvoicePdfService pdfService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getPdfUrl(@PathVariable String invoiceId) {
        String url = pdfService.getPresignedUrl(invoiceId);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
