package com.service.lacasita.controllers;

import com.service.lacasita.dto.PermissionDTO;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.dto.UserPermissionsDTO;
import com.service.lacasita.entity.Permission;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.entity.UserPermissionOverride;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.user.PermissionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;
    private final UserRespository userRepository;

    public PermissionController(PermissionService permissionService, UserRespository userRepository) {
        this.permissionService = permissionService;
        this.userRepository = userRepository;
    }

    /*
     * Permisos del usuario autenticado.
     */
    @PostMapping("/my-permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<UserPermissionsDTO>> getMyPermissions(
            @RequestBody(required = false) UserId body, Authentication auth) {

        Long userId = userRepository.findByUsername(auth.getName())
                .map(UserModel::getId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", 401));

        UserPermissionsDTO permissions = permissionService.getUserPermissions(userId);

        return new ResponseEntity<>(
                new Response<>(permissions, new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    /**
     * Obtiene los permisos de un rol específico (solo ADMIN).
     */
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<PermissionDTO>>> getPermissionsByRole(@PathVariable Long roleId) {

        List<PermissionDTO> permissions = permissionService.getPermissionsByRole(roleId);

        return new ResponseEntity<>(
                new Response<>(permissions, new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    /**
     * Guardar permisos de un rol (solo ADMIN).
     * Recibe una lista de Permission entities.
     */
    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<String>> savePermissions(@RequestBody List<Permission> permissions) {

        permissionService.saveAllPermissions(permissions);

        return new ResponseEntity<>(
                new Response<>("Permisos actualizados", new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    // =====================================================================
    // Excepciones por usuario
    // =====================================================================

    /** Excepciones de un empleado, vigentes y caducadas, para administrarlas. */
    @PostMapping("/user/find")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<UserPermissionOverride>>> getUserOverrides(@RequestBody UserId body) {

        return new ResponseEntity<>(
                new Response<>(permissionService.getOverrides(body.getId()), new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    /** Excepciones vigentes de todo el personal: para el repaso periódico. */
    @PostMapping("/user/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<UserPermissionOverride>>> getActiveOverrides() {

        return new ResponseEntity<>(
                new Response<>(permissionService.getAllActiveOverrides(), new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    /**
     * Crea o actualiza una excepción.
     */
    @PostMapping("/user/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<UserPermissionOverride>> saveUserOverride(
            @RequestBody UserPermissionOverride body, Authentication auth) {

        Long adminId = userRepository.findByUsername(auth.getName())
                .map(UserModel::getId)
                .orElse(null);

        UserPermissionOverride saved = permissionService.saveOverride(body, adminId);

        return new ResponseEntity<>(
                new Response<>(saved, new StatusCode(200, "Excepción guardada")),
                HttpStatus.OK);
    }

    /** Revoca una excepción: el usuario vuelve a lo que diga su rol. */
    @PostMapping("/user/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<String>> deleteUserOverride(@RequestBody UserPermissionOverride body) {

        permissionService.deleteOverride(body.getUserId(), body.getModuleKey());

        return new ResponseEntity<>(
                new Response<>("Excepción revocada", new StatusCode(200, "OK")),
                HttpStatus.OK);
    }
}
