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

/**
 * Un insumo dentro de una sesión de conteo.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "conteo_linea")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConteoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "conteo_id", nullable = false)
    private Conteo conteo;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "saldo_sistema", nullable = false, precision = 14, scale = 3)
    private BigDecimal saldoSistema;

    @Column(precision = 14, scale = 3)
    private BigDecimal contado;

    /** Costo al abrir, para valorar la diferencia aunque el promedio cambie luego. */
    @Column(name = "unit_cost", precision = 14, scale = 4)
    private BigDecimal unitCost;
}
