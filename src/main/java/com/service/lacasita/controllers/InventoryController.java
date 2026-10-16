package com.service.lacasita.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;

import com.service.lacasita.dto.InventoryMovementRequest;
import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.ReposicionProveedor;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.Inventory;
import com.service.lacasita.entity.InventoryMovement;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.inventory.InventoryService;
import com.service.lacasita.service.inventory.ReplenishmentService;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Inventario.
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventory", description = "Gestión de insumos y movimientos de stock")
@PreAuthorize("@perm.lee(authentication, 'inventory')")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ReplenishmentService replenishmentService;

    @Autowired
    private UserRespository userRepository;

    @PostMapping("/consultinventory")
    public ResponseEntity<PageResponse<Inventory>> findAllInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String category) {

        Page<Inventory> inventory = (term == null || term.isBlank()) && (category == null || category.isBlank())
                ? inventoryService.findAllInventory(page, size)
                : inventoryService.search(term, category, page, size);

        return new ResponseEntity<>(
                new PageResponse<>(inventory, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/find-all-inventory")
    public ResponseEntity<Response<List<Inventory>>> getInventory() {
        List<Inventory> response = inventoryService.getAllInventory();
        return new ResponseEntity<>(new Response<>(response, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/item/id")
    public ResponseEntity<Response<Inventory>> getInventoryById(@RequestBody UserId id) {
        Inventory response = inventoryService.getInventoryById(id.getId());
        return new ResponseEntity<>(new Response<>(response, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    /** Insumos en o por debajo del punto de reposición. */
    @PostMapping("/low-stock")
    @PreAuthorize("@perm.lee(authentication, 'inventory')")
    public ResponseEntity<Response<List<Inventory>>> getLowStock() {
        return new ResponseEntity<>(
                new Response<>(inventoryService.getLowStock(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }
    
    /** Bajo el mínimo, agrupado por proveedor, con cuánto pedir. */
    @PostMapping("/reposicion")
    @PreAuthorize("@perm.lee(authentication, 'inventory')")
    public ResponseEntity<Response<List<ReposicionProveedor>>> reposicion() {
        return new ResponseEntity<>(
                new Response<>(replenishmentService.sugerencias(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Valorización del inventario al costo.
     */
    @PostMapping("/stock-value")
    @PreAuthorize("@perm.lee(authentication, 'inventory')")
    public ResponseEntity<Response<BigDecimal>> getStockValue() {
        return new ResponseEntity<>(
                new Response<>(inventoryService.getTotalStockValue(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Historial de movimientos de un insumo: el "por qué" de cada cambio de saldo. */
    @PostMapping("/movements")
    @PreAuthorize("@perm.lee(authentication, 'inventory')")
    public ResponseEntity<PageResponse<InventoryMovement>> getMovements(
            @RequestBody UserId body,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<InventoryMovement> movements = inventoryService.getMovements(body.getId(), page, size);
        return new ResponseEntity<>(
                new PageResponse<>(movements, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }
    

    @PostMapping(value = "/create", consumes = { "multipart/form-data" })
    @PreAuthorize("@perm.puede(authentication, 'inventory', 'write')")
    public ResponseEntity<Response<Inventory>> create(
            @RequestPart("item") Inventory body,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication auth) {

        body.setImage(inventoryService.subirImagen(file));

        Inventory created = inventoryService.createInventory(body, currentUserId(auth));
        return new ResponseEntity<>(new Response<>(created, new StatusCode(200, "Successfully")), HttpStatus.CREATED);
    }

    /**
     * Edición de datos maestros, con imagen opcional.
     *
     * <p>Sin archivo, la imagen que ya tenía se conserva: el servicio solo la pisa
     * cuando llega una nueva.
     */
    @PostMapping(value = "/update", consumes = { "multipart/form-data" })
    @PreAuthorize("@perm.puede(authentication, 'inventory', 'write')")
    public ResponseEntity<Response<Inventory>> update(
            @RequestPart("item") Inventory body,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        body.setImage(inventoryService.subirImagen(file));

        Inventory updated = inventoryService.updateInventory(body);
        return new ResponseEntity<>(new Response<>(updated, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    /** Baja lógica: la fila se conserva porque el histórico de movimientos la referencia. */
    @PostMapping("/deactivate")
    @PreAuthorize("@perm.puede(authentication, 'inventory', 'delete')")
    public ResponseEntity<Response<Inventory>> deactivate(@RequestBody UserId body) {
        Inventory deactivated = inventoryService.deactivateInventory(body.getId());
        return new ResponseEntity<>(new Response<>(deactivated, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    // ---------------------------------------------------------------------
    // Movimientos
    // ---------------------------------------------------------------------

    @PostMapping("/movement/purchase")
    @PreAuthorize("@perm.puede(authentication, 'inventory', 'write')")
    public ResponseEntity<Response<InventoryMovement>> purchase(
            @RequestBody InventoryMovementRequest body, Authentication auth) {

        InventoryMovement movement = inventoryService.registerPurchase(
                body.getInventoryId(), body.getQuantity(), body.getUnitCost(),
                body.getReason(), currentUserId(auth));

        return new ResponseEntity<>(new Response<>(movement, new StatusCode(200, "Successfully")), HttpStatus.CREATED);
    }

    /** Merma: la puede registrar cocina, que es quien la detecta. */
    @PostMapping("/movement/waste")
    @PreAuthorize("@perm.puede(authentication, 'inventory', 'write')")
    public ResponseEntity<Response<InventoryMovement>> waste(
            @RequestBody InventoryMovementRequest body, Authentication auth) {

        InventoryMovement movement = inventoryService.registerWaste(
                body.getInventoryId(), body.getQuantity(), body.getReason(), currentUserId(auth));

        return new ResponseEntity<>(new Response<>(movement, new StatusCode(200, "Successfully")), HttpStatus.CREATED);
    }

    /**
     * Ajuste por conteo físico. {@code quantity} es el saldo REAL contado; el
     * backend calcula y registra la diferencia.
     */
    @PostMapping("/movement/adjust")
    @PreAuthorize("@perm.esAdmin(authentication)")
    public ResponseEntity<Response<InventoryMovement>> adjust(
            @RequestBody InventoryMovementRequest body, Authentication auth) {

        InventoryMovement movement = inventoryService.adjustStock(
                body.getInventoryId(), body.getQuantity(), body.getReason(), currentUserId(auth));

        return new ResponseEntity<>(new Response<>(movement, new StatusCode(200, "Successfully")), HttpStatus.CREATED);
    }

    // ---------------------------------------------------------------------

    /** Id del usuario autenticado, para dejar firmado quién hizo cada movimiento. */
    private Long currentUserId(Authentication auth) {
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .map(UserModel::getId)
                .orElse(null);
    }
}
