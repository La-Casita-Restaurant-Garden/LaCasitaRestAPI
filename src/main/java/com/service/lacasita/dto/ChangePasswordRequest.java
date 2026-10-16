package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición de cambio de contraseña de un empleado.
 * @author Jason Nunez
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    private Long userId;
    private String newPassword;
}
