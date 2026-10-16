package com.service.lacasita.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.service.lacasita.entity.Compra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompraResumen {

    private Long       id;
    private String     numeroFactura;
    private Long       proveedorId;
    private String     proveedorNombre;
    private LocalDate  fecha;
    private String     estado;
    private String     condicion;
    private BigDecimal total;

    public static CompraResumen de(Compra c) {
        return new CompraResumen(
                c.getId(),
                c.getNumeroFactura(),
                c.getProveedor() != null ? c.getProveedor().getId() : null,
                c.getProveedor() != null ? c.getProveedor().getNombre() : null,
                c.getFecha(),
                c.getEstado(),
                c.getCondicion(),
                c.getTotal()
        );
    }
}
