package com.service.lacasita.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprobante
 * @author Jason Nunez
 */
public record TicketResponse(

        Long            orderId,
        String          orderNumber,
        LocalDateTime   fecha,
        String          estado,
        String          cliente,
        String          telefono,
        List<Linea>     lineas,
        BigDecimal      subtotal,
        List<Descuento> descuentos,
        BigDecimal      totalDescuentos,
        BigDecimal      impuesto,
        BigDecimal      envio,
        BigDecimal      total,
        BigDecimal      totalAbonado,
        String          metodoPago,
        boolean         esCredito,
        BigDecimal      saldoCliente,
        BigDecimal      efectivoRecibido,
        BigDecimal      vuelto) {

    public record Linea(
            String     nombre,
            Integer    cantidad,
            BigDecimal precio,
            BigDecimal descuento,
            BigDecimal total
     ) {}

    public record Descuento(
            String     nombre,
            BigDecimal porcentaje,
            BigDecimal monto,
            String     documento
        ) {}
}
