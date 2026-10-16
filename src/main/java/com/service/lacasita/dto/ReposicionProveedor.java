package com.service.lacasita.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReposicionProveedor {

    private Long                 proveedorId;
    private String               proveedorNombre;
    private List<ReposicionItem> items;
}
