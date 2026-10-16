package com.service.lacasita.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un depósito contra una cotización en borrador.
 *
 * <p>Mismo espíritu que {@link CreditMovement}: las filas no se editan ni se
 * borran, y el detalle vive aparte del checkout normal porque una cotización
 * todavía no es una venta.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "cotizacion_pago")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago_id")
    private Long metodoPagoId;

    private String nota;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
