package com.service.lacasita.controllers;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.SaleSearchRequest;
import com.service.lacasita.dto.SaleSummary;
import com.service.lacasita.dto.SalesChartDTO;
import com.service.lacasita.dto.SalesResponse;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.service.orders.OrderService;
import com.service.lacasita.service.sales.SaleService;
import com.service.lacasita.service.sales.SalesChartService;

import java.time.LocalDate;

@RestController
@RequestMapping("/sales")
public class SalesController {

    private final SaleService saleService;
    private final SalesChartService salesChartService;
    private final OrderService orderService;

    public SalesController(SaleService saleService, SalesChartService salesChartService, OrderService orderService) {
        this.saleService = saleService;
        this.salesChartService = salesChartService;
        this.orderService = orderService;
    }

    /**
     * Lista unificada de Ventas
     */
    @PostMapping("/search")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<PageResponse<SaleSummary>> buscarVentas(
            @RequestBody(required = false) SaleSearchRequest body,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String search = body != null ? body.getSearch() : null;
        String status = body != null ? body.getStatus() : null;
        Long paymentMethodId = body != null ? body.getPaymentMethodId() : null;
        String desde = body != null ? body.getDesde() : null;
        String hasta = body != null ? body.getHasta() : null;

        Page<SaleSummary> ventas = orderService.buscarVentas(status, search, paymentMethodId, desde, hasta, page, size);

        return new ResponseEntity<>(
                new PageResponse<>(ventas, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Obtener todas las ventas paginadas
     */
    @PostMapping("/find-all-sales")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<PageResponse<SalesResponse>> getSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<SalesResponse> sales = saleService.findAllSales(page, size);

        return new ResponseEntity<>(
                new PageResponse<>(sales, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Obtener ventas por cliente
     */
    @PostMapping("/sales-customers")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<PageResponse<SalesResponse>> getSalesCustomers(
            @RequestBody UserId body,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<SalesResponse> sales = saleService.findAllSalesByCustomerId(body.getId(), page, size);

        return new ResponseEntity<>(
                new PageResponse<>(sales, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /**
     * Datos para la gráfica de ventas mensuales.
     * Servicio separado que no depende de SalesResponse.
     */
    @GetMapping("/chart/monthly")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<Response<SalesChartDTO>> getMonthlySalesChart(
            @RequestParam(defaultValue = "0") int year) {

        if (year == 0) {
            year = LocalDate.now().getYear();
        }

        SalesChartDTO chart = salesChartService.getMonthlySalesChart(year);

        return new ResponseEntity<>(
                new Response<>(chart, new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    /**
     * Datos para la gráfica de ventas diarias de un mes.
     */
    @GetMapping("/chart/daily")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<Response<SalesChartDTO>> getDailySalesChart(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {

        if (year == 0) {
            year = LocalDate.now().getYear();
        }
        if (month == 0) {
            month = LocalDate.now().getMonthValue();
        }

        SalesChartDTO chart = salesChartService.getDailySalesChart(month, year);

        return new ResponseEntity<>(
                new Response<>(chart, new StatusCode(200, "OK")),
                HttpStatus.OK);
    }
}
