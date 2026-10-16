package com.service.lacasita.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.dto.RecipeLine;
import com.service.lacasita.entity.ProductRecipe;

@Repository
public interface ProductRecipeRepository
        extends JpaRepository<ProductRecipe, ProductRecipe.ProductRecipeId> {

    List<ProductRecipe> findByProductId(UUID productId);

    /** Recetas de varios productos en una sola consulta (evita N+1 al cerrar una orden). */
    @Query("SELECT r FROM ProductRecipe r WHERE r.productId IN :productIds")
    List<ProductRecipe> findByProductIdIn(@Param("productIds") List<UUID> productIds);

    /** Productos afectados si un insumo se agota. */
    List<ProductRecipe> findByInventoryId(Long inventoryId);

    void deleteByProductId(UUID productId);

    /**
     * La receta con el insumo ya resuelto: nombre, unidad y existencia.
     *
     * <p>En una consulta y no cargando cada insumo aparte. La pantalla necesita
     * la <b>unidad</b> junto a cada cantidad —es lo que evita teclear 150 gramos
     * sobre un insumo medido en kilos— y no hay relación mapeada entre las dos
     * entidades: {@code ProductRecipe} guarda el id suelto a propósito, para que
     * el listener de consumo no arrastre el insumo entero en cada pedido.
     */
    @Query("""
            SELECT new com.service.lacasita.dto.RecipeLine(
                     i.id, i.name, i.unit, r.qtyPerUnit, i.quantity)
              FROM ProductRecipe r, Inventory i
             WHERE i.id = r.inventoryId
               AND r.productId = :productId
             ORDER BY i.name
            """)
    List<RecipeLine> findDetalleByProductId(@Param("productId") UUID productId);
}
