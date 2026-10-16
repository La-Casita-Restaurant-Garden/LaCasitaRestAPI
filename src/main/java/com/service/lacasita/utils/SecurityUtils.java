package com.service.lacasita.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


public final class SecurityUtils {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    private SecurityUtils() {
    }


    public static boolean esAdmin() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROL_ADMIN::equals);
    }
}
