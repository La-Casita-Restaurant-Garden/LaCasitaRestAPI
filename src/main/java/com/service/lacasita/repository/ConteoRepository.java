package com.service.lacasita.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.Conteo;

@Repository
public interface ConteoRepository extends JpaRepository<Conteo, Long> {

    /**
     * Solo puede haber uno; la base lo refuerza con un indice unico parcial. Trae
     * las líneas de una vez: es la que se usa para capturar el conteo.
     */
    @Query("SELECT c FROM Conteo c LEFT JOIN FETCH c.lineas WHERE c.estado = 'ABIERTO'")
    Optional<Conteo> findAbierto();

    @Query("SELECT c FROM Conteo c LEFT JOIN FETCH c.lineas WHERE c.id = :id")
    Optional<Conteo> findConLineas(@Param("id") Long id);

    @Query("SELECT c FROM Conteo c ORDER BY c.fecha DESC, c.id DESC")
    List<Conteo> findRecientes();
}
