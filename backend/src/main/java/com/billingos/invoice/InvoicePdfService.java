package com.billingos.invoice;

import com.billingos.certificate.StorageService;
import com.billingos.common.exception.ResourceNotFoundException;
import com.billingos.company.Company;
import com.billingos.company.CompanyRepository;
import com.billingos.customer.Customer;
import com.billingos.customer.CustomerRepository;
import com.billingos.dte.InvoiceDte;
import com.billingos.dte.InvoiceDteRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private static final String FILE_TYPE        = "INVOICE_PDF";
    private static final int    PRESIGN_SECONDS  = 60;

    private final InvoiceRepository       invoiceRepository;
    private final InvoiceDteRepository    dteRepository;
    private final CustomerRepository      customerRepository;
    private final CompanyRepository       companyRepository;
    private final DocumentFileRepository  documentFileRepository;
    private final StorageService          storageService;
    private final SpringTemplateEngine    templateEngine;

    @Transactional
    public void generateAndStore(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        InvoiceDte dte = dteRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("DTE not found for invoice: " + invoiceId));

        Customer customer = customerRepository.findById(invoice.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + invoice.getCustomerId()));

        Company company = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No company configured"));

        byte[] pdf = renderPdf(invoice, dte, customer, company);
        String hash = sha256Hex(pdf);

        String storageKey = "invoices/" + invoiceId + "/invoice.pdf";
        storageService.store(storageKey, new ByteArrayInputStream(pdf), pdf.length, "application/pdf");

        documentFileRepository.findTopByInvoiceIdAndFileTypeOrderByCreatedAtDesc(invoiceId, FILE_TYPE)
                .ifPresent(existing -> documentFileRepository.delete(existing));

        DocumentFile doc = new DocumentFile();
        doc.setInvoiceId(invoiceId);
        doc.setInvoiceDteId(dte.getId());
        doc.setFileType(FILE_TYPE);
        doc.setStorageKey(storageKey);
        doc.setContentHash(hash);
        documentFileRepository.save(doc);

        log.info("PDF generated and stored for invoice {} → {}", invoiceId, storageKey);
    }

    @Transactional(readOnly = true)
    public String getPresignedUrl(String invoiceId) {
        DocumentFile doc = documentFileRepository
                .findTopByInvoiceIdAndFileTypeOrderByCreatedAtDesc(invoiceId, FILE_TYPE)
                .orElseThrow(() -> new ResourceNotFoundException("PDF not yet available for invoice: " + invoiceId));
        return storageService.presignedUrl(doc.getStorageKey(), PRESIGN_SECONDS);
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private byte[] renderPdf(Invoice invoice, InvoiceDte dte, Customer customer, Company company) {
        Context ctx = new Context(Locale.forLanguageTag("es-SV"));
        ctx.setVariable("invoice", invoice);
        ctx.setVariable("dte", dte);
        ctx.setVariable("customer", customer);
        ctx.setVariable("company", company);

        // Thymeleaf needs java.time support — use Instant converted to ZonedDateTime for formatting
        ctx.setVariable("invoiceDateLocal",
                invoice.getInvoiceDate().atZone(ZoneId.of("America/El_Salvador")));
        if (dte.getAcceptedAt() != null) {
            ctx.setVariable("acceptedAtLocal",
                    dte.getAcceptedAt().atZone(ZoneId.of("America/El_Salvador")));
        }

        String html = templateEngine.process("invoice-pdf", ctx);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render PDF for invoice " + invoice.getId(), e);
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
