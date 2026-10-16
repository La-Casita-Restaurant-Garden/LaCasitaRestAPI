package com.service.lacasita.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * La receta completa de un producto.
 *
 * <p>Se guarda <b>entera</b>, no línea a línea: el servidor borra las que había y
 * escribe las que llegan. Con altas y bajas sueltas, una pantalla que se quedó
 * abierta mientras otro editaba acabaría mezclando dos versiones de la receta sin
 * que nadie lo note. Es el mismo criterio que ya usan los descuentos del pedido.
 *
 * @author Jason Nunez
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequest {

    private UUID productId;

    private List<Linea> lineas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Linea {
        private Long inventoryId;
        private BigDecimal qtyPerUnit;
    }
}
