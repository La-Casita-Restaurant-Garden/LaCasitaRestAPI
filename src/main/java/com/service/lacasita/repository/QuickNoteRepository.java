package com.service.lacasita.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.QuickNote;

@Repository
public interface QuickNoteRepository extends JpaRepository<QuickNote, Long> {

    /** Las que se ofrecen al tomar un pedido. */
    List<QuickNote> findByActivoTrueOrderByOrdenAscIdAsc();

    /** Todas, para la pantalla de administración: también las apagadas. */
    List<QuickNote> findAllByOrderByActivoDescOrdenAscIdAsc();

    /** El texto es único: sirve para detectar duplicados antes de insertar. */
    Optional<QuickNote> findByTextoIgnoreCase(String texto);
}
