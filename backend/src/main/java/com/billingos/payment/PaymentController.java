package com.billingos.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto.PaymentResponse create(@Valid @RequestBody PaymentDto.CreatePaymentRequest req) {
        return paymentService.record(req);
    }

    @GetMapping("/invoices/{invoiceId}/payments")
    public List<PaymentDto.PaymentResponse> listByInvoice(@PathVariable String invoiceId) {
        return paymentService.listByInvoice(invoiceId);
    }
}
