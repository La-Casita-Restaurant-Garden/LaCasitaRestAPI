package com.service.lacasita.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_recipe")
@IdClass(ProductRecipe.ProductRecipeId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecipe {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Id
    @Column(name = "inventory_id")
    private Long inventoryId;

    /** Cantidad de insumo por unidad vendida, en la unidad del insumo. */
    @Column(name = "qty_per_unit", nullable = false, precision = 14, scale = 4)
    private BigDecimal qtyPerUnit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRecipeId implements Serializable {
        private UUID productId;
        private Long inventoryId;
    }
}
