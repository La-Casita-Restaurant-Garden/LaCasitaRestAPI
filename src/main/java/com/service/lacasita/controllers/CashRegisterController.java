package com.service.lacasita.controllers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.CashCloseSummary;
import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.entity.CashSession;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.cash.CashRegisterService;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Caja: apertura, cierre y cuadre del turno.
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/cash")
@Tag(name = "Cash", description = "Cierre de caja")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterService cashService;
    private final UserRespository userRepository;

    /** La caja abierta, o null. Es lo primero que consulta la pantalla. */
    @PostMapping("/session")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<Response<CashSession>> sesionAbierta() {
        return new ResponseEntity<>(
                new Response<>(cashService.getSesionAbierta(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/open")
    @PreAuthorize("@perm.puede(authentication, 'sales', 'write')")
    public ResponseEntity<Response<CashSession>> abrir(
            @RequestBody Map<String, Object> body, Authentication auth) {

        CashSession sesion = cashService.abrir(
                aDecimal(body.get("fondoInicial"), "fondo inicial"), currentUserId(auth));

        return new ResponseEntity<>(
                new Response<>(sesion, new StatusCode(200, "Caja abierta")), HttpStatus.OK);
    }

    /** Estado de la caja abierta sin cerrarla: sirve para ir mirando durante el turno. */
    @PostMapping("/summary")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<Response<CashCloseSummary>> resumen() {
        return new ResponseEntity<>(
                new Response<>(cashService.resumenActual(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/close")
    @PreAuthorize("@perm.puede(authentication, 'sales', 'write')")
    public ResponseEntity<Response<CashSession>> cerrar(
            @RequestBody Map<String, Object> body, Authentication auth) {

        CashSession sesion = cashService.cerrar(
                aDecimal(body.get("efectivoContado"), "efectivo contado"),
                body.get("notas") != null ? body.get("notas").toString() : null,
                currentUserId(auth));

        return new ResponseEntity<>(
                new Response<>(sesion, new StatusCode(200, "Caja cerrada")), HttpStatus.OK);
    }

    /** Resumen de un rango cualquiera, para revisar días ya cerrados. */
    @PostMapping("/range")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<Response<CashCloseSummary>> rango(@RequestBody Map<String, Object> body) {

        if (body.get("desde") == null || body.get("hasta") == null) {
            throw new BusinessException("Indique el rango de fechas", 400);
        }

        return new ResponseEntity<>(
                new Response<>(cashService.resumenDe(
                        Instant.parse(body.get("desde").toString()),
                        Instant.parse(body.get("hasta").toString())),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Historial de cierres: donde se ven las diferencias acumuladas. */
    @PostMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CashSession>> historial(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<CashSession> sesiones = cashService.getHistorial(page, size);

        return new ResponseEntity<>(
                new PageResponse<>(sesiones, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    // ---------------------------------------------------------------------

    private BigDecimal aDecimal(Object valor, String campo) {
        if (valor == null) {
            throw new BusinessException("Falta el " + campo, 400);
        }
        try {
            return new BigDecimal(valor.toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BusinessException("El %s no es un número válido: %s".formatted(campo, valor), 400);
        }
    }

    private Long currentUserId(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).map(UserModel::getId).orElse(null);
    }
}
