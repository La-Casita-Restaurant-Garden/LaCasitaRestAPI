package com.service.lacasita.service.orders;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.entity.QuickNote;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.QuickNoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * Catálogo de notas rápidas de comanda.
 *
 * <p>Se consulta en cada alta de pedido y cambia muy de vez en cuando, así que el
 * listado va cacheado; toda escritura invalida el caché.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class QuickNoteService {

    private final QuickNoteRepository repository;

    /** Las que se ofrecen al tomar un pedido. */
    @Cacheable("quickNotes")
    @Transactional(readOnly = true)
    public List<QuickNote> getActivas() {
        return repository.findByActivoTrueOrderByOrdenAscIdAsc();
    }

    /** Todas, incluidas las apagadas: es lo que administra Configuraciones. */
    @Transactional(readOnly = true)
    public List<QuickNote> getTodas() {
        return repository.findAllByOrderByActivoDescOrdenAscIdAsc();
    }

    @Transactional
    @CacheEvict(value = "quickNotes", allEntries = true)
    public QuickNote guardar(QuickNote nota) {

        String texto = nota.getTexto() != null ? nota.getTexto().trim() : "";

        if (texto.isEmpty()) {
            throw new BusinessException("El texto de la nota es obligatorio", 400);
        }

        if (texto.length() > QuickNote.MAX_TEXTO) {
            throw new BusinessException(
                    "La nota no puede pasar de " + QuickNote.MAX_TEXTO + " caracteres", 400);
        }

        // Duplicado sin distinguir mayúsculas: "Sin sal" y "sin sal" son la misma
        // nota y tener las dos solo ensucia la fila de chips.
        repository.findByTextoIgnoreCase(texto).ifPresent(existente -> {
            if (!existente.getId().equals(nota.getId())) {
                throw new BusinessException("Ya existe una nota con ese texto", 409);
            }
        });

        nota.setTexto(texto);
        if (nota.getOrden() == null) nota.setOrden(0);
        if (nota.getActivo() == null) nota.setActivo(true);

        return repository.save(nota);
    }

    /** Guarda el orden de toda la lista de una vez, tras reordenarla en la UI. */
    @Transactional
    @CacheEvict(value = "quickNotes", allEntries = true)
    public void reordenar(List<QuickNote> notas) {
        for (int i = 0; i < notas.size(); i++) {
            QuickNote entrante = notas.get(i);
            QuickNote actual = repository.findById(entrante.getId())
                    .orElseThrow(() -> new BusinessException("Nota no encontrada", 404));
            // Múltiplos de 10 para poder intercalar una nota nueva sin renumerar.
            actual.setOrden((i + 1) * 10);
            repository.save(actual);
        }
    }

    @Transactional
    @CacheEvict(value = "quickNotes", allEntries = true)
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new BusinessException("Nota no encontrada", 404);
        }
        repository.deleteById(id);
    }
}
