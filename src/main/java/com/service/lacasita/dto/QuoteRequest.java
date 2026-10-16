package com.service.lacasita.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición sobre una cotización que no es la orden completa: rechazarla o
 * registrar un depósito contra ella.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuoteRequest {

    private Long orderId;

    /** Importe del depósito. */
    private BigDecimal monto;

    /** Con qué método se recibió el depósito. */
    private Long metodoPagoId;

    private String nota;

    /** Motivo del rechazo. */
    private String motivo;

    /** Plantilla a usar al enviar/previsualizar el correo; null = la primera activa. */
    private Long plantillaId;

    /** Destinatario del correo; en blanco = el correo del cliente de la orden. */
    private String email;
}
