package com.service.lacasita.dto;

import java.time.LocalDateTime;

/**
 * Una fila de la lista unificada de Ventas: la orden es la venta, sea cual
 * sea su estado (DRAFT incluido). Proyección JPQL, no carga la entidad completa.
 *
 * @author Jason Nunez
 */
public record SaleSummary(

        Long orderId,
        String orderNumber,
        String status,
        String quoteStage,
        Double total,
        LocalDateTime fecha,

        String clienteNombre,
        String clienteApellido,

        String metodoPago,

        String creadoNombre,
        String creadoApellido) {
}
