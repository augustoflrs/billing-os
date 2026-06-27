package com.billingos.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class BillableItemController {

    private final BillableItemService itemService;

    @GetMapping
    public ItemDto.PageResponse list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return itemService.list(search, type, page, size);
    }

    @GetMapping("/{id}")
    public ItemDto.Response get(@PathVariable String id) {
        return itemService.get(id);
    }

    @PostMapping
    public ResponseEntity<ItemDto.Response> create(@Valid @RequestBody ItemDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
    }

    @PutMapping("/{id}")
    public ItemDto.Response update(
            @PathVariable String id,
            @Valid @RequestBody ItemDto.Request request,
            @RequestHeader("X-Expected-Version") Long expectedVersion) {
        return itemService.update(id, request, expectedVersion);
    }

    @PostMapping("/{id}/prices")
    public ResponseEntity<ItemDto.PriceResponse> addPrice(
            @PathVariable String id,
            @Valid @RequestBody ItemDto.PriceInput request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.addPrice(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        itemService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
