package com.billingos.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillableItemRepository extends JpaRepository<BillableItem, String> {

    @Query("""
            SELECT i FROM BillableItem i
            WHERE i.active = true
              AND (:type IS NULL OR :type = '' OR i.itemType = :type)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(i.sku, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(i.code, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY i.name ASC
            """)
    Page<BillableItem> search(@Param("search") String search,
                              @Param("type") String type,
                              Pageable pageable);
}
