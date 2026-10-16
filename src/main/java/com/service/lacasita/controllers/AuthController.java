package com.service.lacasita.controllers;

import com.service.lacasita.dto.LoginRequest;
import com.service.lacasita.dto.RefreshTokenRequest;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.auth.AuthService;
import com.service.lacasita.dto.AuthResponseDTO;
import com.service.lacasita.ratelimit.AccountLockoutService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.service.lacasita.utils.JwtUtil;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authorization", description = "Order management operations")
public class AuthController {
    
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRespository userRepository;
    private final AccountLockoutService accountLockoutService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, UserRespository userRepository,
            AccountLockoutService accountLockoutService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.accountLockoutService = accountLockoutService;
    }

    @PostMapping("health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<Response<String>>  healthCheck() {

        return new ResponseEntity<>(new Response<>("Healthy", new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

       /**
     * Login: Retorna access token (30 min) + refresh token (24h o 30 días segun 'remember').
     * NO se guarda el password en el cliente. Solo los tokens.
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return tokens")
    public ResponseEntity<Response<AuthResponseDTO>> login(@RequestBody LoginRequest payload) {

        String username = payload.getUsername();

        if (!accountLockoutService.tryAcquire(username)) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(new Response<>(null, new StatusCode(423,
                            "Cuenta bloqueada temporalmente por demasiados intentos fallidos")));
        }

        Optional<UserModel> userdb = authService.authenticate(username, payload.getPassword());

        if (userdb.isPresent()) {
            UserModel user     = userdb.get();
            String    roleName = user.getRoleId() != null ? user.getRoleId().getName() : "USER";
            boolean   remember = payload.getRemember() != null && payload.getRemember();

            accountLockoutService.reset(username);

            String accessToken  = jwtUtil.generateAccessToken(user.getUsername(), user.getId().toString(), roleName);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId().toString(), remember);

            return ResponseEntity.ok(
                    new Response<>(new AuthResponseDTO(accessToken, refreshToken), new StatusCode(200, "Successfully")));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response<>(null, new StatusCode(401, "Invalid credentials")));
        }
    }


    /**
     * Refresh: Valida el refresh token y emite un nuevo access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using a valid refresh token")
    public ResponseEntity<Response<AuthResponseDTO>> refreshToken(@RequestBody RefreshTokenRequest payload) {

        String refreshToken = payload.getRefreshToken();

        // Validar que sea un refresh token válido
        if (!jwtUtil.isValidRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response<>(null, new StatusCode(401, "Refresh token inválido o expirado")));
        }

       
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String userId   = jwtUtil.getIdFromToken(refreshToken);

    
        Optional<UserModel> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response<>(null, new StatusCode(401, "Usuario no encontrado")));
        }

        UserModel user = userOpt.get();
        String roleName = user.getRoleId() != null ? user.getRoleId().getName() : "USER";

        // Generar nuevo access token
        String newAccessToken = jwtUtil.generateAccessToken(username, userId, roleName);

        
        return ResponseEntity.ok(
                new Response<>(new AuthResponseDTO(newAccessToken, refreshToken), new StatusCode(200, "Token renovado")));
    }

    @PostMapping("/password/encode")
    public String hashPassword(@RequestBody LoginRequest body) {
        return authService.decodePassword(body.getPassword());
    }


}
