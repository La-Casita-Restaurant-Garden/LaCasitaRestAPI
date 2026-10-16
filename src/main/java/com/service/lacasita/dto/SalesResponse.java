package com.service.lacasita.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesResponse {
    
    private Long idventa;
    private LocalDateTime fecha;
    private Double total;
    private String name;
    private String status;
    private Long order;
    private String orderNumber;
    private String paymentMethod;
    private Long idusuario;

    /** Viene de la orden, no de `idusuario` (nunca se llena). */
    private String createdByName;
}
