package com.service.lacasita.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.service.lacasita.service.user.PermissionService;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author Jason Nunez
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionChecker {

    private static final Logger log = LoggerFactory.getLogger(PermissionChecker.class);

    private final PermissionService permissionService;

    /**
     * @param action {@code read}, {@code write} o {@code delete}
     * @return true si el usuario autenticado puede hacerlo; ADMIN siempre puede
     */
    public boolean puede(Authentication authentication, String moduleKey, String action) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            boolean permitido = permissionService.puede(authentication.getName(), moduleKey, action);

            if (!permitido && log.isDebugEnabled()) {
                log.debug("Denegado: {} no tiene {}:{}", authentication.getName(), moduleKey, action);
            }
            return permitido;

        } catch (Exception e) {
            log.error("Error resolviendo permisos de {} sobre {}:{}",
                    authentication.getName(), moduleKey, action, e);
            return false;
        }
    }

    /** Atajo para lectura, que es el caso mayoritario. */
    public boolean lee(Authentication authentication, String moduleKey) {
        return puede(authentication, moduleKey, "read");
    }

    public boolean esAdmin(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            return permissionService.esAdmin(authentication.getName());
        } catch (Exception e) {
            log.error("Error resolviendo si {} es admin", authentication.getName(), e);
            return false;
        }
    }
}
