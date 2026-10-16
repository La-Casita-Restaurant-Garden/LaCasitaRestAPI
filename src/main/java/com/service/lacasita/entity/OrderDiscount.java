package com.service.lacasita.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

/**
 * Descuento efectivamente aplicado a un pedido.
 *
 * <p>Un descuento que no deja rastro es indistinguible de un robo. Esta fila es lo
 * que permite que el ticket lo desglose y que el cierre de caja pueda explicar por
 * qué la caja no cuadra con el precio de carta.
 *
 * <p>Guarda el <b>porcentaje vigente en el momento</b> y el <b>importe rebajado</b>,
 * no una referencia a recalcular: si mañana el descuento de jubilado cambia de
 * porcentaje, este pedido conserva el que se le aplicó. Mismo principio que el
 * total congelado del pedido y que {@code venta}.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "orden_descuento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "tipo_descuento_id", nullable = false)
    private Long tipoDescuentoId;

    /**
     * Producto sobre el que recae. Null = descuento de cuenta.
     *
     * <p>Apunta al producto y no a la línea del pedido porque editar una orden
     * borra y recrea sus líneas con identificadores nuevos: una referencia a la
     * línea quedaría colgando en cuanto alguien corrigiera una cantidad.
     */
    @Column(name = "product_id")
    private UUID productId;

    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 30)
    private String documento;

    @Column(length = 255)
    private String motivo;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
