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
 * Una línea del libro de la cuenta del cliente.
 *
 * <p>Las filas no se editan ni se borran. Una corrección es un {@link #TIPO_AJUSTE}
 * nuevo, igual que en el inventario: sin eso no se puede responder "¿por qué debe
 * 45?" ni auditar una condonación.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "movimiento_credito")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditMovement {

    /** Consumo a crédito: sube el saldo. */
    public static final String TIPO_CARGO = "CARGO";
    /** El cliente paga: baja el saldo. */
    public static final String TIPO_ABONO = "ABONO";
    /** Corrección o condonación; el signo lo lleva el monto. */
    public static final String TIPO_AJUSTE = "AJUSTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Saldo resultante, para auditar sin recalcular la historia entera. */
    @Column(name = "saldo_despues", precision = 12, scale = 2)
    private BigDecimal saldoDespues;

    /** Pedido que originó el cargo. Nulo en abonos y ajustes manuales. */
    @Column(name = "order_id")
    private Long orderId;

    /** Con qué método pagó el abono. */
    @Column(name = "idpago")
    private Long idPago;

    private String nota;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
