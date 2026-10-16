package com.service.lacasita.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompraRequest {


    private Long      id;
    private Long      proveedorId;
    private String    numeroFactura;
    private LocalDate fecha;
    private String    condicion;
    private Long      idpago;
    private String    nota;

    private List<Linea> lineas = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Linea {

        private Long       inventoryId;
        private BigDecimal cantidad; /** En la unidad de COMPRA: 5 cajas. */
        private BigDecimal costo;
    }
}
