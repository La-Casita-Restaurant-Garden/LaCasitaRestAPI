package com.service.lacasita.service.inventory;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.entity.Inventory;
import com.service.lacasita.entity.InventoryMovement;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.utils.CostoPromedio;
import com.service.lacasita.repository.InventoryMovementRepository;
import com.service.lacasita.repository.InventoryRespository;
import com.service.lacasita.service.notifications.NotificationService;
import com.service.lacasita.service.s3.S3Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Inventario basado en libro de movimientos.
 *
 * Regla central: el saldo nunca se escribe con un número calculado en Java. Toda
 * variación pasa por {@link #registerMovement}, que ajusta el stock con un UPDATE
 * condicional atómico y deja la línea de auditoría correspondiente.
 *
 * @author Jason Nunez
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private InventoryRespository inventoryRespository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private S3Service s3Service;


    @Transactional(readOnly = true)
    public List<Inventory> getAllInventory() {
        return inventoryRespository.findAll(Sort.by("name").ascending());
    }

    @Transactional(readOnly = true)
    public Page<Inventory> findAllInventory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return inventoryRespository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Inventory> search(String term, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return inventoryRespository.search(term, category, pageable);
    }

    @Transactional(readOnly = true)
    public Inventory getInventoryById(Long id) {
        return inventoryRespository.findById(id)
                .orElseThrow(() -> new BusinessException("Insumo no encontrado: " + id, 404));
    }

    @Transactional(readOnly = true)
    public List<Inventory> getLowStock() {
        return inventoryRespository.findLowStock();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalStockValue() {
        return inventoryRespository.totalStockValue();
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovement> getMovements(Long inventoryId, int page, int size) {
        return movementRepository.findByInventoryIdOrderByCreatedAtDesc(
                inventoryId, PageRequest.of(page, size));
    }

    // ---------------------------------------------------------------------
    // Alta y edición
    // ---------------------------------------------------------------------

    /**
     * Sube la imagen del insumo y devuelve su clave.
     *
     * <p>Misma vía que los productos: S3 y se guarda solo la clave, no el binario.
     * Devuelve null si no hay archivo, para que el llamador conserve la que había.
     */
    public String subirImagen(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) return null;

        try {
            return s3Service.uploadFileToS3(archivo);
        } catch (IOException e) {
            throw new BusinessException("No se pudo subir la imagen del insumo", 500);
        }
    }

    /**
     * Alta de insumo. La existencia inicial entra como movimiento, no como campo
     * suelto, para que el libro cuadre desde la primera fila.
     */
    @Transactional
    public Inventory createInventory(Inventory inventory, Long userId) {

        validate(inventory);

        BigDecimal initialQty = inventory.getQuantity() != null ? inventory.getQuantity() : BigDecimal.ZERO;

        inventory.setId(null);
        inventory.setVersion(null);
        inventory.setQuantity(BigDecimal.ZERO);
        normalizarPresentacion(inventory);
        if (inventory.getStatus() == null) {
            inventory.setStatus(Inventory.STATUS_AVAILABLE);
        }

        Inventory saved = inventoryRespository.save(inventory);

        if (initialQty.signum() > 0) {
            registerMovement(saved.getId(), InventoryMovement.TYPE_PURCHASE, initialQty,
                    saved.getUnitCost(), null, "Existencia inicial", userId);
            saved = getInventoryById(saved.getId());
        }

        return saved;
    }

    /**
     * Edición de datos maestros.
     *
     * La cantidad NO se toca aquí a propósito: cambiar el stock es siempre un
     * movimiento con su motivo ({@link #adjustStock}). Si se permitiera editarla
     * desde el formulario, el libro y el saldo se desincronizarían en el primer
     * teclazo.
     */
    @Transactional
    public Inventory updateInventory(Inventory input) {

        Inventory stored = getInventoryById(input.getId());
        validate(input);

        String imagenAnterior = stored.getImage();
        boolean imagenNueva = input.getImage() != null && !input.getImage().isBlank();

        stored.setName(input.getName());
        stored.setCategoria(input.getCategoria());
        stored.setDescription(input.getDescription());
        if (imagenNueva) {
            stored.setImage(input.getImage());
        }
        stored.setPrice(input.getPrice());
        stored.setUnit(input.getUnit() != null ? input.getUnit() : stored.getUnit());
        stored.setMinStock(input.getMinStock() != null ? input.getMinStock() : stored.getMinStock());
        stored.setUnitCost(input.getUnitCost());
        stored.setSupplierId(input.getSupplierId());
        stored.setPurchaseUnit(input.getPurchaseUnit());
        stored.setPurchaseFactor(input.getPurchaseFactor());
        normalizarPresentacion(stored);
        stored.setStatus(input.getStatus() != null ? input.getStatus() : stored.getStatus());

        Inventory guardado = inventoryRespository.save(stored);

        if (imagenNueva && imagenAnterior != null && !imagenAnterior.equals(guardado.getImage())) {
            s3Service.deleteFileFromS3(imagenAnterior);
        }

        return guardado;
    }


    private void normalizarPresentacion(Inventory item) {

        if (item.getPurchaseFactor() == null || item.getPurchaseFactor().signum() <= 0) {
            item.setPurchaseFactor(BigDecimal.ONE);
        }

        if (item.getPurchaseUnit() != null && item.getPurchaseUnit().isBlank()) {
            item.setPurchaseUnit(null);
        }
    }

    @Transactional
    public Inventory deactivateInventory(Long id) {
        Inventory stored = getInventoryById(id);
        stored.setStatus(Inventory.STATUS_INACTIVE);
        return inventoryRespository.save(stored);
    }

    // ---------------------------------------------------------------------
    // Movimientos
    // ---------------------------------------------------------------------

    /** Entrada por compra. */
    @Transactional
    public InventoryMovement registerPurchase(Long inventoryId, BigDecimal qty, BigDecimal unitCost,
            String reason, Long userId) {
        requirePositive(qty);
        return registerMovement(inventoryId, InventoryMovement.TYPE_PURCHASE, qty, unitCost, null, reason, userId);
    }

    /** Salida por merma, caducidad o rotura. */
    @Transactional
    public InventoryMovement registerWaste(Long inventoryId, BigDecimal qty, String reason, Long userId) {
        requirePositive(qty);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("La merma requiere un motivo", 400);
        }
        return registerMovement(inventoryId, InventoryMovement.TYPE_WASTE, qty.negate(), null, null, reason, userId);
    }

    /** Corrección tras conteo físico: se informa el saldo real y se registra la diferencia. */
    @Transactional
    public InventoryMovement adjustStock(Long inventoryId, BigDecimal countedQty, String reason, Long userId) {

        if (countedQty == null || countedQty.signum() < 0) {
            throw new BusinessException("La cantidad contada no puede ser negativa", 400);
        }

        Inventory item = getInventoryById(inventoryId);
        BigDecimal delta = countedQty.subtract(item.getQuantity());

        if (delta.signum() == 0) {
            throw new BusinessException("El conteo coincide con el saldo actual: no hay ajuste que registrar", 400);
        }

        return registerMovement(inventoryId, InventoryMovement.TYPE_ADJUSTMENT, delta, null, null,
                reason != null ? reason : "Ajuste por conteo físico", userId);
    }

    /**
     * Núcleo del inventario: aplica el movimiento y ajusta el saldo atómicamente.
     *
     * @param quantity positivo entra, negativo sale
     */
    @Transactional
    public InventoryMovement registerMovement(Long inventoryId, String type, BigDecimal quantity,
            BigDecimal unitCost, Long orderId, String reason, Long userId) {

        return registerMovement(inventoryId, type, quantity, unitCost, orderId, null, reason, userId);
    }

    /** Igual que el anterior, atando el movimiento a la compra que lo originó. */
    @Transactional
    public InventoryMovement registerMovement(Long inventoryId, String type, BigDecimal quantity,
            BigDecimal unitCost, Long orderId, Long compraId, String reason, Long userId) {

        return registerMovement(inventoryId, type, quantity, unitCost, orderId, compraId, null, reason, userId);
    }

    /** Igual que el anterior, atando el movimiento al conteo que lo originó. */
    @Transactional
    public InventoryMovement registerMovement(Long inventoryId, String type, BigDecimal quantity,
            BigDecimal unitCost, Long orderId, Long compraId, Long conteoId, String reason, Long userId) {

        if (quantity == null || quantity.signum() == 0) {
            throw new BusinessException("La cantidad del movimiento no puede ser cero", 400);
        }

        Inventory item = getInventoryById(inventoryId);


        BigDecimal saldoAnterior = item.getQuantity();
        BigDecimal costoAnterior = item.getUnitCost();

        if (quantity.signum() < 0) {
            BigDecimal salida = quantity.negate();

            int updated = inventoryRespository.decrementStock(inventoryId, salida);
            if (updated == 0) {
                throw new BusinessException(
                        "Stock insuficiente de '%s': disponible %s %s, solicitado %s %s".formatted(
                                item.getName(), item.getQuantity(), item.getUnit(), salida, item.getUnit()),
                        409);
            }
        } else {
            inventoryRespository.incrementStock(inventoryId, quantity);
        }

        Inventory refreshed = getInventoryById(inventoryId);

        BigDecimal costoDelMovimiento = quantity.signum() < 0 && unitCost == null
                ? costoAnterior
                : unitCost;

        InventoryMovement movement = movementRepository.save(InventoryMovement.builder()
                .inventoryId(inventoryId)
                .type(type)
                .quantity(quantity)
                .balanceAfter(refreshed.getQuantity())
                .unitCost(costoDelMovimiento)
                .orderId(orderId)
                .compraId(compraId)
                .conteoId(conteoId)
                .reason(reason)
                .createdBy(userId)
                .build());

        if (quantity.signum() > 0) {
            BigDecimal nuevoCosto = CostoPromedio.recalcular(saldoAnterior, costoAnterior, quantity, unitCost);

            if (nuevoCosto != null && !nuevoCosto.equals(costoAnterior)) {
                refreshed.setUnitCost(nuevoCosto);
                inventoryRespository.save(refreshed);
            }
        }

        notifyIfBelowMinimum(refreshed);

        return movement;
    }


    /**
     * Avisa a administración cuando el insumo cruza su punto de reposición. Reutiliza
     * el mismo canal WebSocket que las órdenes: llega al Manager sin recargar.
     */
    private void notifyIfBelowMinimum(Inventory item) {
        if (!item.isBelowMinimum() || Inventory.STATUS_INACTIVE.equals(item.getStatus())) {
            return;
        }
        try {
            notificationService.notifyLowStock(
                    item.getName(),
                    "Quedan %s %s (mínimo %s). Reponer.".formatted(
                            item.getQuantity(), item.getUnit(), item.getMinStock()));
        } catch (Exception e) {
            // El movimiento ya se registró: un fallo al avisar no debe revertirlo.
            log.warn("No se pudo notificar stock bajo de '{}': {}", item.getName(), e.getMessage());
        }
    }

    private void validate(Inventory inventory) {
        if (inventory.getName() == null || inventory.getName().isBlank()) {
            throw new BusinessException("El nombre del insumo es obligatorio", 400);
        }
        if (inventory.getMinStock() != null && inventory.getMinStock().signum() < 0) {
            throw new BusinessException("El stock mínimo no puede ser negativo", 400);
        }
        if (inventory.getUnitCost() != null && inventory.getUnitCost().signum() < 0) {
            throw new BusinessException("El costo unitario no puede ser negativo", 400);
        }
    }

    private void requirePositive(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("La cantidad debe ser mayor que cero", 400);
        }
    }
}
