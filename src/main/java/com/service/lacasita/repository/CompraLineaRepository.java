package com.service.lacasita.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.CompraLinea;

@Repository
public interface CompraLineaRepository extends JpaRepository<CompraLinea, Long> {

    @Query("SELECT l FROM CompraLinea l WHERE l.compra.id = :compraId")
    List<CompraLinea> findPorCompra(@Param("compraId") Long compraId);
}
