package com.service.lacasita.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición de aplicar un descuento a un pedido.
 *
 * <p>El cliente elige <b>cuál</b> y <b>sobre qué</b>; el importe lo calcula el
 * servidor a partir del tipo. Es la misma regla que con los precios: si el monto
 * viniera del navegador volveríamos al agujero que se cerró el 2026-07-31, esta vez
 * con una rebaja arbitraria en vez de un precio arbitrario.
 *
 * <p>La única excepción es el descuento de importe libre —la cortesía—, donde el
 * tipo no define cifra y alguien tiene que escribirla. Por eso ese tipo exige
 * motivo y permiso de ADMIN.
 *
 * @author Jason Nunez
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRequest {

    /** Tipo del catálogo {@code tipo_descuento}. */
    private Long tipoId;

    /**
     * Producto sobre el que recae, para los descuentos de línea.
     *
     * <p>Null significa descuento de cuenta: va sobre el consumo completo. Es el
     * caso del jubilado, que es un derecho de quien consume y no una rebaja de un
     * plato concreto.
     */
    private UUID productId;

    /** Solo se tiene en cuenta si el tipo es de importe libre. */
    private Double monto;

    /** Cédula del beneficiario, cuando el tipo la exige. */
    private String documento;

    private String motivo;
}
