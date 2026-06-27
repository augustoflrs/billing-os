package com.billingos.invoice;

import com.billingos.invoice.InvoiceDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoiceService.list(search, page, size);
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
    public InvoiceResponse cancel(@PathVariable String id) {
        return invoiceService.cancel(id);
    }
}
