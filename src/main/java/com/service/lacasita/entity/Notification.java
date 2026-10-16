package com.service.lacasita.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    public static final String TIPO_NUEVA_ORDEN = "NEW_ORDER";
    public static final String TIPO_STOCK_BAJO  = "LOW_STOCK";
    public static final String TIPO_ACCESO      = "ACCESS_REQUEST";
    public static final String TIPO_GENERAL     = "GENERAL";

    private Long idNotificacion;
    private Long idUsuario;
    private LocalDateTime fecha;
    private String titulo;
    private String mensaje;
    private Boolean leido;
    private String orderNumber;
    private Long orderId;
    private String tipo;
    private LocalDateTime creado_en;
}
