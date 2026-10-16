package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para los datos de la gráfica de ventas.
 * Contiene las etiquetas (labels) y los valores (data) para la gráfica.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesChartDTO {

    private List<String> labels;
    private List<Double> data;
    private Double totalRevenue;
    private Double averageSale;
    private Long totalTransactions;
}
