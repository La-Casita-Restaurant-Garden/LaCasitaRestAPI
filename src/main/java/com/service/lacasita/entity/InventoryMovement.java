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

@Entity
@Table(name = "inventory_movement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovement {

    /** Entrada por compra a proveedor (cantidad positiva). */
    public static final String TYPE_PURCHASE = "PURCHASE";
    /** Devolución de insumo al almacén (cantidad positiva). */
    public static final String TYPE_RETURN = "RETURN";
    /** Consumo por una orden servida (cantidad negativa). */
    public static final String TYPE_CONSUMPTION = "CONSUMPTION";
    /** Merma, caducidad o rotura (cantidad negativa). */
    public static final String TYPE_WASTE = "WASTE";
    /** Corrección tras conteo físico (cualquier signo). */
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(nullable = false, length = 20)
    private String type;

    /** Positivo entra, negativo sale. Nunca cero (lo impide un CHECK en la BD). */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    /** Saldo resultante, para auditar sin recalcular toda la historia. */
    @Column(name = "balance_after", precision = 14, scale = 3)
    private BigDecimal balanceAfter;

    @Column(name = "unit_cost", precision = 14, scale = 4)
    private BigDecimal unitCost;

    /** Orden que originó el consumo, cuando aplica. */
    @Column(name = "order_id")
    private Long orderId;

    /** Compra que originó la entrada. Es lo que permite anular una factura entera. */
    @Column(name = "compra_id")
    private Long compraId;

    /** Conteo que originó el ajuste. Impide que cerrar dos veces ajuste dos veces. */
    @Column(name = "conteo_id")
    private Long conteoId;

    private String reason;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
