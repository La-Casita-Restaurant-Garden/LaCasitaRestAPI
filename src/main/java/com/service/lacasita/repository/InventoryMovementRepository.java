package com.service.lacasita.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.InventoryMovement;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId, Pageable pageable);

    Page<InventoryMovement> findByOrderIdOrderByCreatedAtDesc(Long orderId, Pageable pageable);

   
    @Query("SELECT COUNT(m) > 0 FROM InventoryMovement m WHERE m.orderId = :orderId AND m.type = 'CONSUMPTION'")
    boolean alreadyConsumedForOrder(@Param("orderId") Long orderId);

    @Query("""
            SELECT m.inventoryId, COALESCE(SUM(-m.quantity), 0)
              FROM InventoryMovement m
             WHERE m.type = 'CONSUMPTION'
               AND m.inventoryId IN :ids
               AND m.createdAt >= :desde
             GROUP BY m.inventoryId
            """)
    List<Object[]> consumoDesde(@Param("ids") List<Long> ids, @Param("desde") Instant desde);
}
