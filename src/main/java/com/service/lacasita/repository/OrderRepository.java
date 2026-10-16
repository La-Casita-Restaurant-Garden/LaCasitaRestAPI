package com.service.lacasita.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.service.lacasita.dto.OrderSummary;
import com.service.lacasita.entity.Customer;
import com.service.lacasita.entity.Order;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {

    /*
     * Method to get orders by customer
     */
    List<Order> findByCustomer(Customer customer);

    /**
     * Todas las órdenes, paginadas y lo más reciente primero.
     *
     * <p>Sin las cotizaciones en borrador: esas viven en Ventas hasta
     * confirmarse — invisibles para cocina/inventario/caja, igual que
     * {@link #findResumenEntre}.
     */
    @Query(value = "SELECT o FROM Order o WHERE o.status IS NULL OR o.status <> 'DRAFT' ORDER BY o.date_placed DESC",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status IS NULL OR o.status <> 'DRAFT'")
    Page<Order> findAllOrderByDatePlacedDesc(Pageable pageable);

    /**
     * Importe de los pedidos de un día.
     */
    @Query("""
            SELECT COALESCE(SUM(o.total), 0.0) FROM Order o
             WHERE o.date_placed >= :desde AND o.date_placed < :hasta
               AND (o.status IS NULL OR o.status <> 'CANCELLED')
            """)
    Double getTotalOrdersToday(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Pedidos de un día, sin contar los anulados. */
    @Query("""
            SELECT COUNT(o) FROM Order o
             WHERE o.date_placed >= :desde AND o.date_placed < :hasta
               AND (o.status IS NULL OR o.status <> 'CANCELLED')
            """)
    int getCountOrders(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);


    @EntityGraph(attributePaths = { "items", "customer", "customer.role", "paymentMethod" }, type = EntityGraphType.LOAD)
    Optional<Order> findByIdempotencyKey(UUID idempotencyKey);

    @EntityGraph(attributePaths = { "items", "customer", "customer.role", "paymentMethod" }, type = EntityGraphType.LOAD)
    @Query("SELECT o FROM Order o WHERE o.orderId = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    /*
     * Method to get orders by customer id
     */
    @EntityGraph(attributePaths = { "items", "customer", "customer.role", "paymentMethod" }, type = EntityGraphType.LOAD)
    @Query("SELECT o FROM Order o WHERE o.customer.customer_id = :customerId")
    Page<Order> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

    /**
     * Órdenes de un estado de cocina dentro de un rango. La pantalla de cocina.
     */
    @EntityGraph(attributePaths = { "items", "customer", "customer.role", "paymentMethod" }, type = EntityGraphType.LOAD)
    @Query("""
            SELECT o FROM Order o
             WHERE o.kitchenStatus = :kitchenStatus
               AND o.date_placed >= :desde AND o.date_placed < :hasta
             ORDER BY o.date_placed ASC
            """)
    List<Order> findByKitchenStatusEntre(@Param("kitchenStatus") String kitchenStatus,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /*
     * Method to get total orders by customer id COUNT
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.customer_id = :customerId")
    int totalOrdersByCustomer(@Param("customerId") Long customerId);

    /** Pedidos por fecha calendario en [startDate, endDate], sin contar los anulados. */
    @Query(value = "SELECT * FROM public.findOrdersByDate(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> findOrdersByDate(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Órdenes de un rango, lo más reciente primero, para la lista del mostrador.
     */
    @Query("""
            SELECT new com.service.lacasita.dto.OrderSummary(
                     o.orderId, o.order_number, o.status, o.total, o.date_placed,
                     c.name, c.last_name, m.name, size(o.items))
              FROM Order o
              LEFT JOIN o.customer c
              LEFT JOIN o.paymentMethod m
             WHERE o.date_placed >= :desde AND o.date_placed < :hasta
               AND (o.status IS NULL OR o.status <> 'DRAFT')
             ORDER BY o.date_placed DESC
            """)
    List<OrderSummary> findResumenEntre(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta, Pageable pageable);

    /** Cotizaciones (órdenes en borrador), lo más reciente primero. */
    @EntityGraph(attributePaths = { "items", "customer", "customer.role", "paymentMethod" }, type = EntityGraphType.LOAD)
    @Query("SELECT o FROM Order o WHERE o.status = 'DRAFT' ORDER BY o.date_placed DESC")
    Page<Order> findCotizaciones(Pageable pageable);

}