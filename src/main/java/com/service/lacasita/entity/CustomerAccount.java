package com.service.lacasita.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cuenta de crédito de un cliente: cuánto puede fiar y cuánto debe.
 
 * @author Jason Nunez
 */
@Entity
@Table(name = "cuenta_cliente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccount {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "limite_credito", nullable = false, precision = 12, scale = 2)
    private BigDecimal limiteCredito;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false)
    private Boolean activo;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Cuánto más puede consumir a crédito ahora mismo. */
    public BigDecimal getDisponible() {
        if (limiteCredito == null || saldo == null) return BigDecimal.ZERO;
        BigDecimal disponible = limiteCredito.subtract(saldo);
        return disponible.signum() > 0 ? disponible : BigDecimal.ZERO;
    }

    public boolean debe() {
        return saldo != null && saldo.signum() > 0;
    }
}
