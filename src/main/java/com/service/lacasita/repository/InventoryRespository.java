package com.service.lacasita.repository;

import org.springframework.stereotype.Repository;
import com.service.lacasita.entity.Inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryRespository extends JpaRepository<Inventory, Long> {

    Page<Inventory> findAll(Pageable pageable);

    /**
     * Descuenta stock de forma atómica.
     *
     * Todo ocurre dentro de un solo UPDATE: la condición {@code quantity >= :qty}
     * la evalúa Postgres sobre la fila ya bloqueada por la propia escritura, así que
     * dos consumos simultáneos del mismo insumo no pueden dejar el stock negativo ni
     * pisarse. El patrón "leer, validar en Java, escribir" que se usaba antes es
     * exactamente lo que produce lost updates bajo carga.
     *
     * @return 1 si descontó, 0 si no había existencia suficiente (no lanza)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Inventory i
               SET i.quantity = i.quantity - :qty
             WHERE i.id = :id
               AND i.quantity >= :qty
            """)
    int decrementStock(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * Suma stock (compras, devoluciones, ajustes positivos). Siempre prospera.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Inventory i
               SET i.quantity = i.quantity + :qty
             WHERE i.id = :id
            """)
    int incrementStock(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** Insumos en o por debajo del punto de reposición. */
    @Query("""
            SELECT i FROM Inventory i
             WHERE i.quantity <= i.minStock
               AND i.status <> 'INACTIVE'
             ORDER BY (i.minStock - i.quantity) DESC
            """)
    List<Inventory> findLowStock();

    @Query("""
            SELECT i FROM Inventory i
             LEFT JOIN i.categoria c
             WHERE (:term IS NULL OR :term = ''
                    OR lower(i.name) LIKE lower(concat('%', :term, '%'))
                    OR lower(c.name) LIKE lower(concat('%', :term, '%')))
               AND (:category IS NULL OR :category = '' OR c.name = :category)
            """)
    Page<Inventory> search(@Param("term") String term,
            @Param("category") String category,
            Pageable pageable);

    /** Valorización total del inventario al costo. */
    @Query("SELECT COALESCE(SUM(i.quantity * i.unitCost), 0) FROM Inventory i WHERE i.unitCost IS NOT NULL")
    BigDecimal totalStockValue();
}
