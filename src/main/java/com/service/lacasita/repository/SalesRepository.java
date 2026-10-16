package com.service.lacasita.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.service.lacasita.entity.Sales;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Ventas.
 *
 * <p><b>Toda consulta que sume importes excluye las anuladas.</b> Una venta
 * cancelada conserva su fila —el histórico tiene que poder explicar que se vendió y
 * luego se anuló— pero no puede seguir contando como ingreso. Antes ninguna
 * consulta miraba el estado, así que un pedido cancelado inflaba la facturación
 * para siempre y en silencio.
 */
@Repository
public interface SalesRepository extends JpaRepository<Sales, Long> {

    /** Estado de una venta anulada; las consultas de importe la descartan. */
    String CANCELLED = "CANCELLED";

    /*
     * Method to get all sales
     */
    Page<Sales> findAll(Pageable pageable);

    /** Venta de una orden, si ya se registró. Una orden tiene como mucho una. */
    Optional<Sales> findByOrder_OrderId(Long orderId);

    /*
     * Method to get sales by customer id
     */
    @Query("SELECT s FROM Sales s WHERE s.customer.customer_id = :customer_id")
    Page<Sales> findAllSalesByCustomerId(@Param("customer_id") Long customer_id, Pageable pageable);

    /*
     * Method to get total sales by customer id SUM
     */
    @Query("""
            SELECT COALESCE(SUM(s.total), 0.0) FROM Sales s
             WHERE s.customer.customer_id = :customer_id
               AND (s.status IS NULL OR s.status <> 'CANCELLED')
            """)
    Double totalSalesByCustomer(@Param("customer_id") Long customer_id);

    /*
     * Queries para gráficas de ventas
     */

    /** Total de ventas agrupadas por mes en un año */
    @Query("""
            SELECT MONTH(s.fecha), COALESCE(SUM(s.total), 0.0) FROM Sales s
             WHERE YEAR(s.fecha) = :year
               AND (s.status IS NULL OR s.status <> 'CANCELLED')
             GROUP BY MONTH(s.fecha) ORDER BY MONTH(s.fecha)
            """)
    List<Object[]> findMonthlySalesByYear(@Param("year") int year);

    /** Conteo de ventas entre fechas */
    @Query("""
            SELECT COUNT(s) FROM Sales s
             WHERE s.fecha >= :startDate AND s.fecha <= :endDate
               AND (s.status IS NULL OR s.status <> 'CANCELLED')
            """)
    Long countSalesBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /** Suma total de ventas entre fechas */
    @Query("""
            SELECT COALESCE(SUM(s.total), 0.0) FROM Sales s
             WHERE s.fecha >= :startDate AND s.fecha <= :endDate
               AND (s.status IS NULL OR s.status <> 'CANCELLED')
            """)
    Double sumTotalBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Ventas de un periodo agrupadas por método de pago: la base del cierre de caja.
     *
     * <p>Devuelve {@code [idpago, cantidad, total]}. El método puede ser null en
     * pedidos antiguos, y se agrupa igual para que el total del cierre cuadre con
     * la suma de sus líneas.
     */
    @Query("""
            SELECT s.idpago, COUNT(s), COALESCE(SUM(s.total), 0.0) FROM Sales s
             WHERE s.fecha >= :desde AND s.fecha <= :hasta
               AND (s.status IS NULL OR s.status <> 'CANCELLED')
             GROUP BY s.idpago
            """)
    List<Object[]> resumenPorMetodo(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /**
     * Subtotal, descuentos e impuesto del periodo.
     *
     * <p>LEFT JOIN a propósito: una venta sin pedido asociado no debe hacer
     * desaparecer al resto del informe.
     */
    @Query("""
            SELECT COALESCE(SUM(o.subtotal), 0.0),
                   COALESCE(SUM(o.descuento), 0.0),
                   COALESCE(SUM(o.impuesto), 0.0)
              FROM Sales s LEFT JOIN s.order o
             WHERE s.fecha >= :desde AND s.fecha <= :hasta
               AND (s.status IS NULL OR s.status <> 'CANCELLED')
            """)
    List<Object[]> desgloseDelPeriodo(@Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    /** Ventas diarias del mes actual */
    @Query("""
            SELECT DAY(s.fecha), COALESCE(SUM(s.total), 0.0) FROM Sales s
             WHERE MONTH(s.fecha) = :month AND YEAR(s.fecha) = :year
               AND (s.status IS NULL OR s.status <> 'CANCELLED')
             GROUP BY DAY(s.fecha) ORDER BY DAY(s.fecha)
            """)
    List<Object[]> findDailySalesByMonth(@Param("month") int month, @Param("year") int year);

}
