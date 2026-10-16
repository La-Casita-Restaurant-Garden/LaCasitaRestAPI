package com.service.lacasita.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Las categorías de un catálogo.
     *
     * <p>Desde V018 el catálogo lo comparten productos e insumos, así que en esta
     * tabla conviven "Batidos" con "Desechables". Se separan por {@code type}
     * (V019); pásale una de las listas de
     * {@link com.service.lacasita.utils.CategoryTypes}, que ya incluyen
     * {@code BOTH} en los dos lados.
     *
     * <p>Antes esto se resolvía contando productos, y una categoría recién creada
     * —sin ninguno todavía— no aparecía en el formulario que tenía que darle el
     * primero: no había forma de sacarla de ahí.
     */
    @Query("SELECT c FROM Category c WHERE c.type IN :tipos ORDER BY c.name")
    List<Category> findPorTipo(@Param("tipos") List<String> tipos);

    /**
     * Cada categoría con cuántos productos e insumos la usan.
     *
     * <p>Ese recuento es lo que hace administrable la pantalla: sin él, cambiar
     * el tipo o borrar una categoría se hace a ciegas y no hay forma de saber a
     * cuántas fichas va a afectar hasta que algo desaparece de una lista.
     *
     * <p>Devuelve {@code [Category, long, long]} por fila.
     */
    @Query("""
            SELECT c,
                   (SELECT COUNT(p) FROM Product p WHERE p.category = c),
                   (SELECT COUNT(i) FROM Inventory i WHERE i.categoria = c)
              FROM Category c
             ORDER BY c.name
            """)
    List<Object[]> findConUso();

    /**
     * Busca por nombre ignorando mayúsculas y espacios sobrantes.
     *
     * <p>Es el mismo criterio con el que V018 evitó duplicar 'Bebidas' y
     * 'bebidas'. Si aquí se comparara exacto, la pantalla dejaría crear otra vez
     * lo que la migración acababa de unificar.
     */
    @Query("SELECT c FROM Category c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:nombre))")
    List<Category> findPorNombre(@Param("nombre") String nombre);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :id")
    long contarProductos(@Param("id") Long id);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.categoria.id = :id")
    long contarInsumos(@Param("id") Long id);
}
