package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar permisos al frontend.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDTO {

    private String moduleKey;
    private String moduleLabel;
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean canDelete;
}
