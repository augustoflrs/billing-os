package com.billingos.item;

import com.billingos.common.UlidGenerator;
import com.billingos.common.exception.ConflictException;
import com.billingos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillableItemService {

    private final BillableItemRepository itemRepository;
    private final ItemPriceRepository priceRepository;

    @Transactional(readOnly = true)
    public ItemDto.PageResponse list(String search, String type, int page, int size) {
        Page<BillableItem> result = itemRepository.search(search, type, PageRequest.of(page, size));
        return new ItemDto.PageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ItemDto.Response get(String id) {
        return toResponse(find(id));
    }

    @Transactional
    public ItemDto.Response create(ItemDto.Request req) {
        BillableItem item = new BillableItem();
        item.setId(UlidGenerator.generate());
        item.setCreatedBy(currentUser());
        applyFields(item, req);

        if (req.price() != null) {
            item.getPrices().add(buildPrice(item, req.price()));
        }
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public ItemDto.Response update(String id, ItemDto.Request req, Long expectedVersion) {
        BillableItem item = find(id);
        if (!item.getVersion().equals(expectedVersion)) {
            throw new ConflictException("Stale version: expected " + expectedVersion
                    + " but found " + item.getVersion() + ". Reload and retry.");
        }
        applyFields(item, req);
        item.setUpdatedAt(OffsetDateTime.now());
        item.setUpdatedBy(currentUser());
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public ItemDto.PriceResponse addPrice(String itemId, ItemDto.PriceInput req) {
        BillableItem item = find(itemId);
        // Deactivate any open-ended price that overlaps
        item.getPrices().stream()
                .filter(p -> p.isActive() && p.getValidTo() == null)
                .forEach(p -> p.setValidTo(req.validFrom()));
        ItemPrice price = buildPrice(item, req);
        item.getPrices().add(price);
        itemRepository.save(item);
        return toPriceResponse(price);
    }

    @Transactional
    public void deactivate(String id) {
        BillableItem item = find(id);
        item.setActive(false);
        item.setUpdatedAt(OffsetDateTime.now());
        item.setUpdatedBy(currentUser());
        itemRepository.save(item);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private BillableItem find(String id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));
    }

    private void applyFields(BillableItem item, ItemDto.Request req) {
        item.setItemType(req.itemType());
        item.setSku(req.sku());
        item.setCode(req.code());
        item.setName(req.name());
        item.setDescription(req.description());
    }

    private ItemPrice buildPrice(BillableItem item, ItemDto.PriceInput req) {
        ItemPrice p = new ItemPrice();
        p.setId(UlidGenerator.generate());
        p.setBillableItem(item);
        p.setUnitPrice(req.unitPrice());
        p.setValidFrom(req.validFrom());
        p.setValidTo(req.validTo());
        return p;
    }

    private ItemDto.PriceResponse toPriceResponse(ItemPrice p) {
        return new ItemDto.PriceResponse(
                p.getId(), p.getUnitPrice(), p.getCurrencyCode(),
                p.getValidFrom(), p.getValidTo(), p.isActive()
        );
    }

    private ItemDto.Response toResponse(BillableItem item) {
        OffsetDateTime now = OffsetDateTime.now();
        List<ItemDto.PriceResponse> prices = item.getPrices().stream()
                .map(this::toPriceResponse).toList();

        ItemDto.PriceResponse current = item.getPrices().stream()
                .filter(p -> p.isActive()
                        && !p.getValidFrom().isAfter(now)
                        && (p.getValidTo() == null || p.getValidTo().isAfter(now)))
                .findFirst()
                .map(this::toPriceResponse)
                .orElse(null);

        return new ItemDto.Response(
                item.getId(), item.getItemType(), item.getSku(), item.getCode(),
                item.getName(), item.getDescription(), item.isActive(), item.getVersion(),
                item.getCreatedAt(), item.getUpdatedAt(), prices, current
        );
    }

    private String currentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
