package com.service.lacasita.service.dashboard;

import com.service.lacasita.dto.DashboardChartDTO;
import com.service.lacasita.repository.OrderItemRepository;
import com.service.lacasita.repository.OrderRepository;
import com.service.lacasita.repository.SalesRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio dedicado para las gráficas del dashboard.
 * Separado del DashboardService para mantener responsabilidades claras.
 * Los datos se cachean con Redis para optimizar rendimiento.
 *
 * @author Jason D. Nuñez
 */
@Service
public class DashboardChartService {

    private final SalesRepository salesRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    private static final String[] MONTH_LABELS = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    private static final DateTimeFormatter ETIQUETA_DIA = DateTimeFormatter.ofPattern("dd/MM");

    public DashboardChartService(SalesRepository salesRepository,
                                  OrderItemRepository orderItemRepository,
                                  OrderRepository orderRepository) {
        this.salesRepository = salesRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Períodos que admite el top de productos.
     *
     * <p>Son los tres horizontes con los que se decide algo distinto: hoy dice qué
     * está saliendo ahora, el mes sirve para pedir a proveedores y el año para
     * revisar la carta.
     */
    public static final String HOY = "HOY";
    public static final String MES = "MES";
    public static final String ANIO = "ANIO";

    /**
     * Datos para todas las gráficas del dashboard.
     *
     * <p>La clave de caché lleva el período además del año: sin él, elegir "hoy"
     * devolvería lo que se hubiera cacheado para "este año".
     */
    @Cacheable(value = "dashboardCharts", key = "#year + '-' + #periodo")
    public DashboardChartDTO getDashboardCharts(int year, String periodo) {

        // 1. Revenue mensual
        List<Object[]> monthlySales = salesRepository.findMonthlySalesByYear(year);
        List<String> revenueLabels = new ArrayList<>();
        List<Double> revenueData = new ArrayList<>();

        double[] monthlyData = new double[12];
        for (Object[] row : monthlySales) {
            int month = ((Number) row[0]).intValue();
            double total = ((Number) row[1]).doubleValue();
            monthlyData[month - 1] = total;
        }
        for (int i = 0; i < 12; i++) {
            revenueLabels.add(MONTH_LABELS[i]);
            revenueData.add(monthlyData[i]);
        }

        // 2. Top productos, acotado al período pedido
        LocalDateTime desde = inicioDelPeriodo(periodo);
        LocalDateTime hasta = LocalDate.now().plusDays(1).atStartOfDay();

        List<Object[]> topProducts = orderItemRepository.findTopProducts(desde, hasta);
        List<String> categoryLabels = new ArrayList<>();
        List<Long> categoryData = new ArrayList<>();

        for (Object[] row : topProducts) {
            categoryLabels.add((String) row[0]);
            categoryData.add(((Number) row[1]).longValue());
        }

        // 3. Órdenes por fecha calendario (últimos 7 días, hoy incluido)
        LocalDate desde7 = LocalDate.now().minusDays(6);
        LocalDate hasta7 = LocalDate.now();

        List<Object[]> ordersByDay = orderRepository.findOrdersByDate(desde7, hasta7);

        // Por fecha, no por día de la semana: agrupar por DOW mezclaba dos
        // ocurrencias del mismo día de la semana en una sola barra.
        Map<LocalDate, Long> conteoPorFecha = new LinkedHashMap<>();
        for (Object[] row : ordersByDay) {
            LocalDate fecha = ((java.sql.Date) row[0]).toLocalDate();
            long count = ((Number) row[1]).longValue();
            conteoPorFecha.put(fecha, count);
        }

        List<String> orderLabels = new ArrayList<>();
        List<Long>   orderData   = new ArrayList<>();

        for (LocalDate dia = desde7; !dia.isAfter(hasta7); dia = dia.plusDays(1)) {
            orderLabels.add(dia.format(ETIQUETA_DIA));
            orderData.add(conteoPorFecha.getOrDefault(dia, 0L));
        }

        return new DashboardChartDTO(revenueLabels, revenueData, categoryLabels, categoryData, orderLabels, orderData);
    }

    /**
     * Desde cuándo se cuenta el top.
     */
    private LocalDateTime inicioDelPeriodo(String periodo) {

        LocalDate hoy = LocalDate.now();

        if (HOY.equalsIgnoreCase(periodo))  return hoy.atStartOfDay();
        if (ANIO.equalsIgnoreCase(periodo)) return hoy.withDayOfYear(1).atStartOfDay();

        return hoy.withDayOfMonth(1).atStartOfDay();
    }
}
