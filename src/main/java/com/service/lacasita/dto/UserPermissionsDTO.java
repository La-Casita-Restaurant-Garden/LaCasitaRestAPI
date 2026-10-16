package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que agrupa el rol del usuario y sus permisos por módulo.
 * Se envía al frontend después del login o al cargar el perfil.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPermissionsDTO {

    private String role;
    private List<PermissionDTO> permissions;
}
