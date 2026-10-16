package com.service.lacasita.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


public record CashCloseSummary(

        Long sesionId,
        Instant desde,
        Instant hasta,

        BigDecimal fondoInicial,

        /** Ventas del periodo, desglosadas por cómo se cobraron. */
        List<LineaMetodo> ventasPorMetodo,
        BigDecimal totalVentas,
        BigDecimal ventasEnEfectivo,

        /** Vendido a crédito: salió mercancía, no entró dinero. */
        BigDecimal fiadoDelPeriodo,

        /** Deuda vieja cobrada. Entra en caja pero no es venta de hoy. */
        List<LineaMetodo> abonosPorMetodo,
        BigDecimal totalAbonos,
        BigDecimal abonosEnEfectivo,

        /** Depósitos de cotizaciones aún en borrador. Entran en caja, no son venta todavía. */
        List<LineaMetodo> depositosCotizacionPorMetodo,
        BigDecimal totalDepositosCotizacion,
        BigDecimal depositosCotizacionEnEfectivo,

        /** Por qué se cobró menos que el precio de carta. */
        List<LineaDescuento> descuentos,
        BigDecimal totalDescuentos,

        BigDecimal subtotalDelPeriodo,
        BigDecimal impuestoCobrado,

        BigDecimal efectivoEsperado) {

    /** Una forma de cobro y cuánto entró por ella. */
    public record LineaMetodo(Long idPago, String metodo, long cantidad, BigDecimal total) {
    }

    public record LineaDescuento(Long tipoId, String nombre, long cantidad, BigDecimal total) {
    }
}
