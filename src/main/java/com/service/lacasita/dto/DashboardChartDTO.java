package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para los datos de las gráficas del dashboard.
 * Incluye datos de ventas mensuales, categorías top y órdenes por día.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardChartDTO {

    private List<String> revenueLabels;
    private List<Double> revenueData;

    private List<String> categoryLabels;
    private List<Long> categoryData;

    private List<String> orderLabels;
    private List<Long> orderData;
}
