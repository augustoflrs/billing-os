package com.billingos.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ItemPriceRepository extends JpaRepository<ItemPrice, String> {

    @Query("""
            SELECT p FROM ItemPrice p
            WHERE p.billableItem.id = :itemId
              AND p.active = true
              AND p.validFrom <= :at
              AND (p.validTo IS NULL OR p.validTo > :at)
            ORDER BY p.validFrom DESC
            """)
    Optional<ItemPrice> findActiveAt(@Param("itemId") String itemId, @Param("at") OffsetDateTime at);
}
