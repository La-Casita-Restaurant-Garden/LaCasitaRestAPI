package com.service.lacasita.service.orders;

import java.math.BigDecimal;
import java.util.List;

import com.service.lacasita.entity.DeliveryType;
import com.service.lacasita.entity.OrderDiscount;

/**
 * Desglose de un pedido, calculado en el servidor.
 *
 * <p>Invariante: {@code total = subtotal - descuento + impuesto + envio}. Se
 * guarda entero en el pedido y no se recalcula al leer, para que el ticket y el
 * cierre de caja puedan reimprimirse dentro de un año dando lo mismo aunque la
 * tasa del ITBMS, los porcentajes de descuento o el monto de un tipo de
 * delivery hayan cambiado.
 *
 * @param subtotal     suma de precio × cantidad, antes de nada
 * @param descuento    rebajas de línea más rebajas de cuenta
 * @param baseGravable parte del neto que corresponde a productos que tributan
 * @param impuesto     ITBMS sobre la base ya descontada
 * @param envio        monto del tipo de delivery elegido; cero si no hay delivery. No tributa.
 * @param total        lo que paga el cliente
 * @param descuentos   qué se aplicó y por cuánto, para el ticket y la auditoría
 * @param tipoDelivery el tipo elegido, para congelarlo en la orden; null si no hay delivery
 *
 * @author Jason Nunez
 */
public record OrderTotals(
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal baseGravable,
        BigDecimal impuesto,
        BigDecimal envio,
        BigDecimal total,
        List<OrderDiscount> descuentos,
        DeliveryType tipoDelivery) {

    public double totalDouble() {
        return total.doubleValue();
    }
}
