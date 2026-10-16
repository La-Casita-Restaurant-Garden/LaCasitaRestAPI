package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición de una categoría.
 * @author Jason Nunez
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {

    private Long id;
    private String name;
    private String type;
    private Boolean mostrarEnPedido;
}
