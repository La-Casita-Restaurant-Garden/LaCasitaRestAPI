package com.service.lacasita.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String ip = clientIp(request);

        if (rateLimitService.allowRequest(ip)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitService.getCapacity()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setContentType("application/json");

            response.getWriter().write(
                    mapper.writeValueAsString(
                            new Response<>("Rate limit exceeded", new StatusCode(429, "Too many requests"))));
        }
    }

    /**
     * IP real del cliente.
     *
     * Detrás del proxy de Render/Cloudflare, {@code getRemoteAddr()} devuelve
     * siempre la IP del proxy: todo el restaurante compartía un único cubo de 60
     * req/min, así que una tablet de cocina refrescando podía dejar a los meseros
     * en 429. El primer valor de X-Forwarded-For es el cliente original.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}