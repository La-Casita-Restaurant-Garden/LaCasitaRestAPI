package com.service.lacasita.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición sobre la cuenta de crédito de un cliente: abrir/ajustar el límite,
 * registrar un abono o hacer un ajuste manual.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditRequest {

    private Long customerId;

    /** Importe del abono o del ajuste. En un ajuste puede ser negativo (condonar). */
    private BigDecimal monto;

    /** Solo al abrir cuenta o cambiar el techo de crédito. */
    private BigDecimal limiteCredito;

    /** Suspender el crédito sin borrar la cuenta ni el histórico. */
    private Boolean activo;

    /** Con qué método pagó el abono. */
    private Long idPago;

    /** Motivo. Obligatorio en los ajustes. */
    private String nota;
}
