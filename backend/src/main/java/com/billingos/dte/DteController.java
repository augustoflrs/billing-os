package com.billingos.dte;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices/{invoiceId}/dte")
@RequiredArgsConstructor
public class DteController {

    private final DteService dteService;

    @GetMapping
    public ResponseEntity<DteDto.DteStatusResponse> status(@PathVariable String invoiceId) {
        DteDto.DteStatusResponse dto = dteService.getByInvoiceId(invoiceId);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @GetMapping("/events")
    public List<DteDto.DteEventResponse> events(@PathVariable String invoiceId) {
        return dteService.getEventsByInvoiceId(invoiceId);
    }

    @PostMapping("/retry")
    public DteDto.DteStatusResponse retry(@PathVariable String invoiceId) {
        return dteService.retry(invoiceId);
    }
}
