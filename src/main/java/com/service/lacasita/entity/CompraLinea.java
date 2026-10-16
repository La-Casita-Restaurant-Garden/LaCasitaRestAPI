package com.service.lacasita.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "compra_linea")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    /** Lo que dice la factura, en la unidad de compra. */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal factor = BigDecimal.ONE;

    /** cantidad x factor: lo que entra al libro, en unidad de consumo. */
    @Column(name = "cantidad_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadBase;

    /** Lo que costó cada presentación. */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal costo;

    @Column(name = "costo_base", nullable = false, precision = 14, scale = 4)
    private BigDecimal costoBase;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
