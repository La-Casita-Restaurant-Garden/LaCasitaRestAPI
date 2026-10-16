package com.service.lacasita.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReposicionItem {

    private Long        inventoryId;
    private String      nombre;
    private String      unidad;
    private BigDecimal  saldoActual;
    private BigDecimal  minStock;
    private BigDecimal  deficit;                /** Lo que falta para llegar al mínimo. Es la cantidad sugerida, no una previsión. */
    private BigDecimal  consumoDiarioPromedio;  /** Consumo diario promedio de los últimos 30 días, como contexto para decidir si pedir de más. */
    private String      purchaseUnit;
    private BigDecimal  purchaseFactor;
    private BigDecimal  ultimoCosto;
}
