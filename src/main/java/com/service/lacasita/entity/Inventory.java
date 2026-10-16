package com.service.lacasita.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category categoria;

    private String status;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    /** Precio de venta / referencia. Distinto del costo de compra. */
    private Double price;

    private String name;
    private String image;
    private String description;

    /** Unidad de medida: UND, KG, LB, L, ML, CAJA... */
    @Column(length = 10, nullable = false)
    private String unit = "UND";

    /** Punto de reposición: por debajo de esto se dispara la alerta de stock bajo. */
    @Column(name = "min_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(name = "unit_cost", precision = 14, scale = 4)
    private BigDecimal unitCost;


    @Column(name = "supplierid")
    private Long supplierId;

    /** Presentación en la que se compra: CAJA, SACO. Nulo = se compra por unidad. */
    @Column(name = "purchase_unit", length = 20)
    private String purchaseUnit;

    /** Cuántas unidades de consumo trae una presentación. Una caja son 24 latas. */
    @Column(name = "purchase_factor", precision = 14, scale = 3)
    private BigDecimal purchaseFactor = BigDecimal.ONE;

    /** Bloqueo optimista para las ediciones desde el Manager. */
    @Version
    @Column(nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updated_at;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant created_at;

    /** True cuando el insumo tocó o cruzó su punto de reposición. */
    public boolean isBelowMinimum() {
        return quantity != null && minStock != null && quantity.compareTo(minStock) <= 0;
    }
}
