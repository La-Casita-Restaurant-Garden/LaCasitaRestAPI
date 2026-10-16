package com.service.lacasita.utils;

import java.util.Date;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utilidad JWT con soporte para access token y refresh token.
 * 
 * - Access Token: corto (30 min), se usa en cada request.
 * - Refresh Token: largo (configurable), se usa solo para renovar el access token.
 */
@Service
public class JwtUtil {


    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 256 bits (32 bytes) codificados en base64");
        }
        
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 30; // 30
    private static final long REFRESH_TOKEN_SHORT = 1000 * 60 * 60 * 24; // 24 horas
    private static final long REFRESH_TOKEN_LONG = 1000L * 60 * 60 * 24 * 30; // 30 días (

    /**
     * Genera un access token.
     */
    public String generateAccessToken(String username, String id, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .setSubject(username)
                .claim("id", id)
                .claim("role", role)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Genera un refresh token.
     * @param remember Si true, el token dura 30 días. Si false, 24 horas.
     */
    public String generateRefreshToken(String username, String id, boolean remember) {
        Date now = new Date();
        long expiration = remember ? REFRESH_TOKEN_LONG : REFRESH_TOKEN_SHORT;
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .claim("id", id)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public String getIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", String.class);
    }

    public String getTokenType(String token) {
        Claims claims = getClaims(token);
        return claims.get("type", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valida que sea un refresh token válido (no expirado y de tipo "refresh").
     */
    public boolean isValidRefreshToken(String token) {
        try {
            if (isExpired(token)) return false;
            String type = getTokenType(token);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        Date expiration = getClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}