package com.service.lacasita.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición de movimiento de inventario (compra, merma o ajuste por conteo).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryMovementRequest {

    private Long       inventoryId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private String     reason;
}
