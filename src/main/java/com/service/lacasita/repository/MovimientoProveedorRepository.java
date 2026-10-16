package com.service.lacasita.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.MovimientoProveedor;

@Repository
public interface MovimientoProveedorRepository extends JpaRepository<MovimientoProveedor, Long> {

    @Query(value = "SELECT m FROM MovimientoProveedor m WHERE m.proveedorId = :id ORDER BY m.createdAt DESC",
           countQuery = "SELECT COUNT(m) FROM MovimientoProveedor m WHERE m.proveedorId = :id")
    Page<MovimientoProveedor> findPorProveedor(@Param("id") Long id, Pageable pageable);

    /** Guarda contra el doble cargo si confirmar se reintenta. */
    @Query("SELECT COUNT(m) > 0 FROM MovimientoProveedor m WHERE m.compraId = :compraId AND m.tipo = 'CARGO'")
    boolean yaTieneCargo(@Param("compraId") Long compraId);
}
