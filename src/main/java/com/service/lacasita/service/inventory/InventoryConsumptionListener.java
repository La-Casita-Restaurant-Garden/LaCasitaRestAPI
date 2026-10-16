package com.service.lacasita.service.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.service.lacasita.entity.InventoryMovement;
import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.OrderItem;
import com.service.lacasita.entity.ProductRecipe;
import com.service.lacasita.event.OrderCompletedEvent;
import com.service.lacasita.repository.InventoryMovementRepository;
import com.service.lacasita.repository.OrderRepository;
import com.service.lacasita.repository.ProductRecipeRepository;

import lombok.RequiredArgsConstructor;

/**
 * Descuenta insumos cuando una orden se completa.
 *
 * Esto es lo que convierte al inventario en un dato vivo: antes era una lista que
 * alguien actualizaba a mano y que dejaba de ser cierta el mismo día.
 *
 * Decisiones que vale la pena conocer:
 *
 *   - Corre en AFTER_COMMIT y en transacción propia (REQUIRES_NEW): la orden ya
 *     está cerrada, así que un problema de stock no puede revertirla. Un plato ya
 *     servido no se "des-sirve" porque el inventario esté descuadrado.
 *
 *   - Es idempotente: si el evento se reprocesa (redeploy, reintento manual del
 *     cambio de estado), la guarda de alreadyConsumedForOrder y el índice único
 *     parcial de la BD impiden descontar dos veces.
 *
 *   - Desde V021 una salida sin stock suficiente NO se rechaza: deja el saldo en
 *     negativo. El plato ya salió, así que perder el movimiento dejaría el mismo
 *     descuadre sin rastro. El negativo se ve en rojo hasta que alguien cuente.
 *
 * @author Jason Nunez
 */
@Component
@RequiredArgsConstructor
public class InventoryConsumptionListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryConsumptionListener.class);

    private final OrderRepository orderRepository;
    private final ProductRecipeRepository recipeRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryService inventoryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCompleted(OrderCompletedEvent event) {

        if (movementRepository.alreadyConsumedForOrder(event.orderId())) {
            log.debug("Orden {} ya descontó inventario; se omite", event.orderNumber());
            return;
        }

        // Con los items en la misma consulta: recorrerlos con findById dispararía
        // una carga lazy extra por cada orden.
        Order order = orderRepository.findByIdWithItems(event.orderId()).orElse(null);
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        // Órdenes anteriores a la migración V003 no tienen product_id: no hay forma
        // fiable de saber qué consumieron, así que se dejan pasar.
        List<UUID> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (productIds.isEmpty()) {
            log.debug("Orden {} sin productos enlazados; no se descuenta inventario", event.orderNumber());
            return;
        }

        // Una sola consulta para todas las recetas de la orden, en vez de una por item.
        Map<UUID, List<ProductRecipe>> recipesByProduct = recipeRepository.findByProductIdIn(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductRecipe::getProductId));

        if (recipesByProduct.isEmpty()) {
            return;
        }

        // Consolidar por insumo: si dos líneas distintas usan el mismo insumo, se
        // hace un único movimiento en vez de dos.
        Map<Long, BigDecimal> consumoPorInsumo = order.getItems().stream()
                .filter(item -> item.getProductId() != null && item.getQuantity() != null)
                .flatMap(item -> recipesByProduct
                        .getOrDefault(item.getProductId(), List.of())
                        .stream()
                        .map(recipe -> Map.entry(
                                recipe.getInventoryId(),
                                recipe.getQtyPerUnit().multiply(BigDecimal.valueOf(item.getQuantity())))))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        BigDecimal::add,
                        java.util.LinkedHashMap::new));

        consumoPorInsumo.forEach((inventoryId, cantidad) -> {
            try {
                inventoryService.registerMovement(
                        inventoryId,
                        InventoryMovement.TYPE_CONSUMPTION,
                        cantidad.negate(),
                        null,
                        event.orderId(),
                        "Consumo por orden " + event.orderNumber(),
                        null);
            } catch (Exception e) {
                log.error("No se pudo descontar el insumo {} de la orden {}: {}",
                        inventoryId, event.orderNumber(), e.getMessage());
            }
        });
    }
}
