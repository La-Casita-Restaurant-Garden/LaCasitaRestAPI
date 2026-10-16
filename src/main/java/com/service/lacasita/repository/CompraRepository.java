package com.service.lacasita.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.Compra;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Query(value = "SELECT c FROM Compra c JOIN FETCH c.proveedor ORDER BY c.fecha DESC, c.id DESC",
           countQuery = "SELECT COUNT(c) FROM Compra c")
    Page<Compra> findPagina(Pageable pageable);


    @Query("SELECT c FROM Compra c LEFT JOIN FETCH c.lineas WHERE c.id = :id")
    Optional<Compra> findConLineas(@Param("id") Long id);

    /**
     * La misma factura del mismo proveedor.
     */
    @Query("""
            SELECT c FROM Compra c
             WHERE c.proveedor.id = :proveedorId
               AND LOWER(TRIM(c.numeroFactura)) = LOWER(TRIM(:numero))
               AND c.estado <> 'ANULADA'
            """)
    List<Compra> findPorFactura(@Param("proveedorId") Long proveedorId, @Param("numero") String numero);

    @Query("SELECT c FROM Compra c JOIN FETCH c.proveedor WHERE c.fecha BETWEEN :desde AND :hasta ORDER BY c.fecha DESC")
    List<Compra> findEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
