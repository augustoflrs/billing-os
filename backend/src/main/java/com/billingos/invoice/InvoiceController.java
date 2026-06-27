package com.billingos.invoice;

import com.billingos.invoice.InvoiceDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody CreateInvoiceRequest req) {
        return invoiceService.create(req);
    }

    @GetMapping
    public InvoicePageResponse list(
            @RequestParam(defaultValue = "")  String search,
            @RequestParam(required = false)   String status,
            @RequestParam(required = false)   String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoiceService.list(search, status, customerId, from, to, page, size);
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable String id) {
        return invoiceService.get(id);
    }

    @PostMapping("/{id}/confirm")
    public InvoiceResponse confirm(@PathVariable String id) {
        return invoiceService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public InvoiceResponse cancel(@PathVariable String id,
                                   @RequestBody(required = false) CancelRequest req) {
        return invoiceService.cancel(id, req != null ? req.reason() : null);
    }

    @GetMapping("/{id}/status-history")
    public java.util.List<InvoiceDto.StatusHistoryEntry> statusHistory(@PathVariable String id) {
        return invoiceService.statusHistory(id);
    }

    record CancelRequest(String reason) {}
}
