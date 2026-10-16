package com.service.lacasita.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.service.lacasita.dto.DashboardChartDTO;
import com.service.lacasita.dto.DashboardKpiDTO;
import com.service.lacasita.dto.DateDTO;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.service.dashboard.DashboardChartService;
import com.service.lacasita.service.dashboard.DashboardService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardChartService dashboardChartService;

    public DashboardController(DashboardService dashboardService, DashboardChartService dashboardChartService) {
        this.dashboardService = dashboardService;
        this.dashboardChartService = dashboardChartService;
    }

    @PostMapping("/kpis")
    @PreAuthorize("@perm.lee(authentication, 'dashboard')")
    public ResponseEntity<Response<DashboardKpiDTO>> getKpis(@RequestBody DateDTO body) {

        String dateString = body.getDate();
        LocalDateTime fechaBusqueda;

        if (dateString.length() == 10) {
            fechaBusqueda = LocalDate.parse(dateString).atStartOfDay();
        } else if (dateString.length() >= 19) {
            fechaBusqueda = LocalDateTime.parse(dateString.substring(0, 19));
        } else {
            throw new IllegalArgumentException("Formato de fecha no soportado: " + dateString);
        }

        DashboardKpiDTO kpis = dashboardService.getKpis(fechaBusqueda);

        return new ResponseEntity<>(
                new Response<>(kpis, new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    /**
     * Endpoint para obtener los datos de las gráficas del dashboard.
     * Revenue mensual, top productos, órdenes por día.
     */
    @GetMapping("/charts")
    @PreAuthorize("@perm.lee(authentication, 'dashboard')")
    public ResponseEntity<Response<DashboardChartDTO>> getCharts(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = DashboardChartService.MES) String periodo) {

        if (year == 0) {
            year = LocalDate.now().getYear();
        }

        DashboardChartDTO charts = dashboardChartService.getDashboardCharts(year, periodo);

        return new ResponseEntity<>(
                new Response<>(charts, new StatusCode(200, "OK")),
                HttpStatus.OK);
    }
}
