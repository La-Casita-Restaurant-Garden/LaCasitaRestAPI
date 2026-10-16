package com.service.lacasita.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.QuotePayment;

@Repository
public interface QuotePaymentRepository extends JpaRepository<QuotePayment, Long> {

    /** Historial de una cotización, lo más reciente primero. */
    List<QuotePayment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * Depósitos de cotización de un periodo, por método de pago.
     *
     * <p>Igual que {@code CreditMovementRepository.abonosPorMetodo}: es dinero que
     * entra en la gaveta antes de que la cotización se convierta en venta.
     *
     * <p>Devuelve {@code [metodoPagoId, cantidad, total]}.
     */
    @Query("""
            SELECT p.metodoPagoId, COUNT(p), COALESCE(SUM(p.monto), 0) FROM QuotePayment p
             WHERE p.createdAt >= :desde AND p.createdAt <= :hasta
             GROUP BY p.metodoPagoId
            """)
    List<Object[]> depositosPorMetodo(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
