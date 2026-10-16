package com.service.lacasita.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

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

/**
 * Una línea del libro de la cuenta del proveedor.
 *
 * <p>Las filas no se editan ni se borran. Una corrección es un AJUSTE nuevo.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "movimiento_proveedor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoProveedor {

    public static final String CARGO = "CARGO";
    public static final String ABONO = "ABONO";
    public static final String AJUSTE = "AJUSTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_despues", precision = 12, scale = 2)
    private BigDecimal saldoDespues;

    @Column(name = "compra_id")
    private Long compraId;

    private Long idpago;

    @Column(length = 255)
    private String nota;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
