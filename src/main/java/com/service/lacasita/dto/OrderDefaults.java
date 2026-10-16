package com.service.lacasita.dto;

/**
 * Cliente y categoría con los que arranca la pantalla de crear orden.
 * @author Jason Nunez
 */
public record OrderDefaults(
        Long clienteDefectoId,
        Long categoriaDefectoId) {
}
