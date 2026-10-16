package com.service.lacasita.service.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.ConteoRequest;
import com.service.lacasita.dto.ConteoResumen;
import com.service.lacasita.entity.Conteo;
import com.service.lacasita.entity.ConteoLinea;
import com.service.lacasita.entity.Inventory;
import com.service.lacasita.entity.InventoryMovement;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.ConteoRepository;
import com.service.lacasita.repository.InventoryRespository;

import lombok.RequiredArgsConstructor;

/**
 * Conteo físico: compara lo que el libro cree contra lo que hay de verdad, y
 * convierte la diferencia en el único ADJUSTMENT que la explica.
 *
 * <p>La línea guarda el saldo del sistema EN EL MOMENTO DE ABRIR. El ajuste que
 * se aplica al cerrar es la diferencia entre lo contado y ese saldo congelado,
 * no el valor contado: escribir el absoluto borraría lo que se haya vendido
 * mientras se contaba.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class PhysicalCountService {

    private final ConteoRepository conteoRepository;
    private final InventoryRespository inventoryRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public Conteo abierto() {
        return conteoRepository.findAbierto().orElse(null);
    }

    @Transactional(readOnly = true)
    public Conteo porId(Long id) {
        return conteoRepository.findConLineas(id)
                .orElseThrow(() -> new BusinessException("El conteo no existe", 404));
    }

    @Transactional(readOnly = true)
    public List<ConteoResumen> recientes() {
        return conteoRepository.findRecientes().stream().map(ConteoResumen::de).toList();
    }

    /** Abre la sesión y congela el saldo de cada insumo activo. */
    @Transactional
    public Conteo abrir(String nota, Long userId) {

        if (conteoRepository.findAbierto().isPresent()) {
            throw new BusinessException("Ya hay un conteo abierto: ciérralo o anúlalo antes de abrir otro", 409);
        }

        List<Inventory> insumos = inventoryRepository.findAll().stream()
                .filter(i -> !Inventory.STATUS_INACTIVE.equals(i.getStatus()))
                .toList();

        if (insumos.isEmpty()) {
            throw new BusinessException("No hay insumos activos que contar", 400);
        }

        Conteo conteo = new Conteo();
        conteo.setNota(nota);
        conteo.setCreatedBy(userId);

        for (Inventory item : insumos) {
            ConteoLinea linea = new ConteoLinea();
            linea.setConteo(conteo);
            linea.setInventoryId(item.getId());
            linea.setSaldoSistema(item.getQuantity());
            linea.setUnitCost(item.getUnitCost());
            conteo.getLineas().add(linea);
        }

        return conteoRepository.save(conteo);
    }

    /** Guarda lo contado. Solo toca `contado`; el resto de la línea es del momento de abrir. */
    @Transactional
    public Conteo guardarLineas(Long conteoId, List<ConteoRequest.Linea> conteos) {

        Conteo conteo = porId(conteoId);
        requireAbierto(conteo);

        for (ConteoRequest.Linea lc : conteos) {
            if (lc.getContado() != null && lc.getContado().signum() < 0) {
                throw new BusinessException("Lo contado no puede ser negativo", 400);
            }
        }

        Map<Long, ConteoLinea> porInsumo = conteo.getLineas().stream()
                .collect(Collectors.toMap(ConteoLinea::getInventoryId, l -> l));

        for (ConteoRequest.Linea lc : conteos) {
            ConteoLinea linea = porInsumo.get(lc.getInventoryId());
            if (linea == null) {
                throw new BusinessException(
                        "El insumo %s no está en este conteo".formatted(lc.getInventoryId()), 400);
            }
            linea.setContado(lc.getContado());
        }

        return conteoRepository.save(conteo);
    }

    /** Cierra: genera un ADJUSTMENT por cada línea contada que no cuadra. Ya no se puede editar. */
    @Transactional
    public Conteo cerrar(Long conteoId, Long userId) {

        Conteo conteo = porId(conteoId);
        requireAbierto(conteo);

        boolean algoContado = conteo.getLineas().stream().anyMatch(l -> l.getContado() != null);
        if (!algoContado) {
            throw new BusinessException("No se ha contado ningún insumo todavía", 400);
        }

        for (ConteoLinea linea : conteo.getLineas()) {

            if (linea.getContado() == null) {
                continue;
            }

            BigDecimal diferencia = linea.getContado().subtract(linea.getSaldoSistema());
            if (diferencia.signum() == 0) {
                continue;
            }

            inventoryService.registerMovement(
                    linea.getInventoryId(),
                    InventoryMovement.TYPE_ADJUSTMENT,
                    diferencia,
                    linea.getUnitCost(),
                    null,
                    null,
                    conteo.getId(),
                    "Ajuste por conteo físico #" + conteo.getId(),
                    userId);
        }

        conteo.setEstado(Conteo.CERRADO);
        conteo.setCerradoAt(Instant.now());

        return conteoRepository.save(conteo);
    }

    /** Solo mientras está ABIERTO: nada se aplicó todavía, así que no hay contrapartida que registrar. */
    @Transactional
    public Conteo anular(Long conteoId) {

        Conteo conteo = porId(conteoId);
        requireAbierto(conteo);

        conteo.setEstado(Conteo.ANULADO);

        return conteoRepository.save(conteo);
    }

    private void requireAbierto(Conteo conteo) {
        if (!Conteo.ABIERTO.equals(conteo.getEstado())) {
            throw new BusinessException(
                    "Este conteo está %s: ya no se puede modificar".formatted(conteo.getEstado().toLowerCase()),
                    409);
        }
    }
}
