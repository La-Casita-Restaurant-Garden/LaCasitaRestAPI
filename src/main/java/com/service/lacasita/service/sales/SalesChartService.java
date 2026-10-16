package com.service.lacasita.service.sales;

import com.service.lacasita.dto.SalesChartDTO;
import com.service.lacasita.repository.SalesRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio dedicado para las gráficas de ventas.
 * Separado del SaleService para mantener responsabilidades claras.
 * Los datos de gráficas se cachean con Redis para optimizar rendimiento.
 *
 * @author Jason D. Nuñez
 */
@Service
public class SalesChartService {

    private final SalesRepository salesRepository;

    private static final String[] MONTH_LABELS = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    public SalesChartService(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    /**
     * Obtiene los datos de la gráfica de ventas mensuales para un año dado.
     * Incluye KPIs: totalRevenue, averageSale, totalTransactions.
     */
    @Cacheable(value = "salesChart", key = "#year")
    public SalesChartDTO getMonthlySalesChart(int year) {

        List<Object[]> monthlySales = salesRepository.findMonthlySalesByYear(year);

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        // Inicializar los 12 meses en 0
        double[] monthlyData = new double[12];

        for (Object[] row : monthlySales) {
            int month = ((Number) row[0]).intValue();
            double total = ((Number) row[1]).doubleValue();
            monthlyData[month - 1] = total;
        }

        double totalRevenue = 0;
        for (int i = 0; i < 12; i++) {
            labels.add(MONTH_LABELS[i]);
            data.add(monthlyData[i]);
            totalRevenue += monthlyData[i];
        }

        // Contar transacciones del año
        LocalDateTime startOfYear = LocalDateTime.of(year, Month.JANUARY, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, Month.DECEMBER, 31, 23, 59, 59);
        Long totalTransactions = salesRepository.countSalesBetween(startOfYear, endOfYear);

        double averageSale = totalTransactions > 0 ? totalRevenue / totalTransactions : 0;

        return new SalesChartDTO(labels, data, totalRevenue, averageSale, totalTransactions);
    }

    /**
     * Obtiene los datos de la gráfica de ventas diarias para un mes dado.
     */
    @Cacheable(value = "salesChartDaily", key = "#month + '-' + #year")
    public SalesChartDTO getDailySalesChart(int month, int year) {

        List<Object[]> dailySales = salesRepository.findDailySalesByMonth(month, year);

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        double totalRevenue = 0;

        for (Object[] row : dailySales) {
            int day = ((Number) row[0]).intValue();
            double total = ((Number) row[1]).doubleValue();
            labels.add(String.valueOf(day));
            data.add(total);
            totalRevenue += total;
        }

        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        Long totalTransactions = salesRepository.countSalesBetween(startOfMonth, endOfMonth);

        double averageSale = totalTransactions > 0 ? totalRevenue / totalTransactions : 0;

        return new SalesChartDTO(labels, data, totalRevenue, averageSale, totalTransactions);
    }
}
