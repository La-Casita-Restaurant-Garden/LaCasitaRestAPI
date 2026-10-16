package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una categoría vista desde la pantalla de administración.
 *
 * @author Jason Nunez
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryView {

    private Long id;
    private String name;
    private String type;
    private boolean mostrarEnPedido;
    private long productos;
    private long insumos;
}
