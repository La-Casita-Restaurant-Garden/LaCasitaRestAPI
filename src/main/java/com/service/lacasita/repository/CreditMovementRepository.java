package com.service.lacasita.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.CreditMovement;

@Repository
public interface CreditMovementRepository extends JpaRepository<CreditMovement, Long> {

    /** Estado de cuenta: lo más reciente primero. */
    Page<CreditMovement> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    /**
     * Cargo asociado a un pedido, si lo hay.
     *
     * <p>Sirve para dos cosas: no duplicar el cargo si el alta se reintenta, y
     * saber cuánto revertir cuando el pedido se cancela o cambia de total.
     */
    @Query("SELECT m FROM CreditMovement m WHERE m.orderId = :orderId AND m.tipo = 'CARGO'")
    Optional<CreditMovement> findCargoDeOrden(@Param("orderId") Long orderId);

    /** Todos los movimientos de un pedido, incluidos los ajustes de reversión. */
    List<CreditMovement> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    /**
     * Fecha del cargo más antiguo del cliente.
     *
     * <p>Con saldo corriente no se puede saber con exactitud qué cargo sigue sin
     * pagar, así que esto es una aproximación FIFO: sirve para ordenar por
     * antigüedad y decidir a quién llamar, no como dato contable.
     */
    @Query("SELECT MIN(m.createdAt) FROM CreditMovement m WHERE m.customerId = :customerId AND m.tipo = 'CARGO'")
    java.time.Instant fechaCargoMasAntiguo(@Param("customerId") Long customerId);

    /**
     * Cobros de deuda de un periodo, por método de pago.
     *
     * <p>Es dinero que entra en la gaveta pero <b>no es venta de hoy</b>: la venta
     * se reconoció el día en que se despachó el pedido. Sin esta línea el cierre
     * daría un faltante justo del importe cobrado.
     *
     * <p>Devuelve {@code [idpago, cantidad, total]}.
     */
    @Query("""
            SELECT m.idPago, COUNT(m), COALESCE(SUM(m.monto), 0) FROM CreditMovement m
             WHERE m.tipo = 'ABONO'
               AND m.createdAt >= :desde AND m.createdAt <= :hasta
             GROUP BY m.idPago
            """)
    List<Object[]> abonosPorMetodo(@Param("desde") java.time.Instant desde,
            @Param("hasta") java.time.Instant hasta);
}
