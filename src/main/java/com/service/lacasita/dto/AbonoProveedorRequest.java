package com.service.lacasita.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbonoProveedorRequest {

    private Long       proveedorId;
    private BigDecimal monto;
    private Long       idpago;
    private String     nota;
}
