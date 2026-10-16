package com.service.lacasita.utils;

/*
 * @author Jason D. Nuñez
 * @version 1.2
 * Valida el token y carga roles del usuario
 */

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;


@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private UserRespository userRepository;

    @Autowired
    private JwtUtil jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        autenticarSiProcede(request);


        filterChain.doFilter(request, response);
    }

    private void autenticarSiProcede(HttpServletRequest request) {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        final String jwt = authHeader.substring(7);

        try {

            if (!jwtService.isTokenValid(jwt)) {
                return;
            }

            if (!"access".equals(jwtService.getTokenType(jwt))) {
                return;
            }

            final String username = jwtService.getUsernameFromToken(jwt);
            if (username == null) {
                return;
            }

            UserModel user = userRepository.findByUsername(username).orElse(null);

            List<SimpleGrantedAuthority> authorities = Collections.emptyList();
            if (user != null && user.getRoleId() != null) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleId().getName()));
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception e) {
            log.debug("Token no utilizable, se continúa sin autenticar: {}", e.getMessage());
        }
    }
}
