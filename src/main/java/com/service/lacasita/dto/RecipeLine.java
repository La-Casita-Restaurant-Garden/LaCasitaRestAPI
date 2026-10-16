package com.service.lacasita.dto;

import java.math.BigDecimal;

/**
 * Una línea de receta, con el insumo ya resuelto.
 *
 * <p>Lleva el <b>nombre y la unidad</b> del insumo además de la cantidad. No es
 * adorno: la unidad la manda el insumo, no la receta, y sin verla al lado del
 * campo es cuestión de tiempo que alguien escriba 150 pensando en gramos sobre
 * un insumo medido en kilos. Ese error vacía el almacén en una sola orden y la
 * base no puede detectarlo, porque 150 es un número perfectamente válido.
 *
 * <p>{@code stockActual} viaja para poder avisar en pantalla cuando una receta
 * consume más de lo que queda.
 *
 * @author Jason Nunez
 */
public record RecipeLine(

        Long inventoryId,
        String insumo,
        String unidad,
        BigDecimal qtyPerUnit,
        BigDecimal stockActual) {
}
