package com.billingos.customer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public CustomerDto.PageResponse list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerService.list(search, page, size);
    }

    @GetMapping("/{id}")
    public CustomerDto.Response get(@PathVariable String id) {
        return customerService.get(id);
    }

    @PostMapping
    public ResponseEntity<CustomerDto.Response> create(@Valid @RequestBody CustomerDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    public CustomerDto.Response update(
            @PathVariable String id,
            @Valid @RequestBody CustomerDto.Request request,
            @RequestHeader("X-Expected-Version") Long expectedVersion) {
        return customerService.update(id, request, expectedVersion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        customerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
