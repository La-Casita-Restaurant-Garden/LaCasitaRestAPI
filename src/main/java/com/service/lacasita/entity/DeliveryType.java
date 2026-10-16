package com.service.lacasita.entity;

import java.math.BigDecimal;
import java.time.Instant;

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
 * Tipo de delivery que el negocio puede cobrar: catálogo de nombre + monto
 * fijo, igual de simple que {@link DiscountType} pero sin alcance ni las
 * exigencias de un descuento — un delivery suma al total, no lo rebaja.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "tipo_delivery")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private Boolean activo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
