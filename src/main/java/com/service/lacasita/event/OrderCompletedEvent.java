package com.service.lacasita.event;

/**
 * Se publica cuando una orden pasa a COMPLETED. Dispara el descuento de insumos
 * del inventario según la receta de cada producto ({@code product_recipe}).
 *
 * Igual que {@link OrderCreatedEvent}, se consume en AFTER_COMMIT: el stock solo
 * baja si la orden realmente quedó completada.
 *
 * @author Jason Nunez
 */
public record OrderCompletedEvent(Long orderId, String orderNumber) {
}
