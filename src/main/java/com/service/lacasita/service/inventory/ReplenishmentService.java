package com.service.lacasita.service.inventory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.ReposicionItem;
import com.service.lacasita.dto.ReposicionProveedor;
import com.service.lacasita.entity.Inventory;
import com.service.lacasita.entity.Proveedor;
import com.service.lacasita.repository.InventoryMovementRepository;
import com.service.lacasita.repository.InventoryRespository;
import com.service.lacasita.repository.ProveedorRepository;

import lombok.RequiredArgsConstructor;

/**
 * Convierte "esto está bajo el mínimo" en "esto hay que pedir, y a quién".
 *
 * <p>La cantidad sugerida es el déficit contra el mínimo, no una previsión de
 * demanda: el consumo diario promedio de los últimos {@link #DIAS_CONSUMO}
 * días se muestra aparte, como dato para que quien compra decida si pide de
 * más, no para que el sistema lo decida por él con una fórmula inventada.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class ReplenishmentService {

    private static final int DIAS_CONSUMO = 30;

    private final InventoryRespository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ReposicionProveedor> sugerencias() {

        List<Inventory> bajos = inventoryRepository.findLowStock();

        if (bajos.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> consumo = consumoDiarioPromedio(bajos);

        Map<Long, String> nombresProveedor = proveedorRepository.findAllById(
                bajos.stream().map(Inventory::getSupplierId).filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Proveedor::getId, Proveedor::getNombre));

        Map<Long, List<ReposicionItem>> porProveedor = new LinkedHashMap<>();

        for (Inventory item : bajos) {
            ReposicionItem linea = new ReposicionItem(
                    item.getId(),
                    item.getName(),
                    item.getUnit(),
                    item.getQuantity(),
                    item.getMinStock(),
                    item.getMinStock().subtract(item.getQuantity()).max(BigDecimal.ZERO),
                    consumo.getOrDefault(item.getId(), BigDecimal.ZERO),
                    item.getPurchaseUnit(),
                    item.getPurchaseFactor(),
                    item.getUnitCost());

            porProveedor.computeIfAbsent(item.getSupplierId(), k -> new ArrayList<>()).add(linea);
        }

        List<ReposicionProveedor> resultado = new ArrayList<>();

        for (Map.Entry<Long, List<ReposicionItem>> entry : porProveedor.entrySet()) {
            Long proveedorId = entry.getKey();
            resultado.add(new ReposicionProveedor(
                    proveedorId,
                    proveedorId == null
                            ? "Sin proveedor asignado"
                            : nombresProveedor.getOrDefault(proveedorId, "Proveedor " + proveedorId),
                    entry.getValue()));
        }

        resultado.sort(Comparator.comparing(
                ReposicionProveedor::getProveedorNombre, Comparator.nullsLast(String::compareTo)));

        return resultado;
    }

    private Map<Long, BigDecimal> consumoDiarioPromedio(List<Inventory> items) {

        List<Long> ids = items.stream().map(Inventory::getId).toList();
        Instant desde = Instant.now().minus(DIAS_CONSUMO, ChronoUnit.DAYS);

        Map<Long, BigDecimal> promedio = new HashMap<>();

        for (Object[] fila : movementRepository.consumoDesde(ids, desde)) {
            BigDecimal total = (BigDecimal) fila[1];
            promedio.put((Long) fila[0], total.divide(BigDecimal.valueOf(DIAS_CONSUMO), 3, RoundingMode.HALF_UP));
        }

        return promedio;
    }
}
