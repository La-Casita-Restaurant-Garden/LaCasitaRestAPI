package com.service.lacasita.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Una línea de la pantalla de deudores.
 *
 * <p>Lleva ya el nombre y el teléfono del cliente: la lista sirve para llamar a
 * quien debe, y obligar al frontend a pedir cada ficha aparte sería un N+1 sobre
 * la pantalla más consultada del módulo.
 */
public record DebtorResponse(
        Long customerId,
        String nombre,
        String telefono,
        BigDecimal saldo,
        BigDecimal limiteCredito,
        BigDecimal disponible,
        Boolean activo,
        Instant desde) 
        {
}
