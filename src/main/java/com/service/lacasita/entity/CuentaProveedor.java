package com.service.lacasita.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "cuenta_proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaProveedor {

    @Id
    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
