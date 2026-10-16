package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filtros de la lista de Ventas: búsqueda libre (número de orden o cliente),
 * estado exacto, método de pago y rango de fechas. Todos opcionales — sin
 * cuerpo trae todo. {@code desde}/{@code hasta} vienen como {@code yyyy-MM-dd}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleSearchRequest {

    private String search;
    private String status;
    private Long paymentMethodId;
    private String desde;
    private String hasta;
}
