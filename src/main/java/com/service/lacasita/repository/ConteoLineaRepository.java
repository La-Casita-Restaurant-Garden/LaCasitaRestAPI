package com.service.lacasita.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.ConteoLinea;

@Repository
public interface ConteoLineaRepository extends JpaRepository<ConteoLinea, Long> {

    @Query("SELECT l FROM ConteoLinea l WHERE l.conteo.id = :conteoId")
    List<ConteoLinea> findPorConteo(@Param("conteoId") Long conteoId);
}
