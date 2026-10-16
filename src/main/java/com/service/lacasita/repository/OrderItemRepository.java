package com.service.lacasita.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.OrderItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrder_OrderId(Long id);

    /**
     * Lo más vendido en un período, por unidades.
     */
    @Query("""
            SELECT oi.name, SUM(oi.quantity)
              FROM OrderItem oi JOIN oi.order o
             WHERE o.date_placed >= :desde AND o.date_placed < :hasta
               AND (o.status IS NULL OR o.status <> 'CANCELLED')
             GROUP BY oi.name
             ORDER BY SUM(oi.quantity) DESC
             LIMIT 10
            """)
    List<Object[]> findTopProducts(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}