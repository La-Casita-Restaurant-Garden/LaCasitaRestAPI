package com.service.lacasita.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    @Query("SELECT p FROM Proveedor p WHERE p.activo = true ORDER BY p.nombre")
    List<Proveedor> findActivos();

    /**
     * Por nombre, ignorando mayúsculas y espacios sobrantes.
     */
    @Query("SELECT p FROM Proveedor p WHERE LOWER(TRIM(p.nombre)) = LOWER(TRIM(:nombre))")
    List<Proveedor> findPorNombre(@Param("nombre") String nombre);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.supplierId = :id")
    long contarInsumos(@Param("id") Long id);

    @Query("SELECT COUNT(c) FROM Compra c WHERE c.proveedor.id = :id")
    long contarCompras(@Param("id") Long id);
}
