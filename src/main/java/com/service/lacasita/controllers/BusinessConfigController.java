package com.service.lacasita.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.BusinessInfo;
import com.service.lacasita.dto.OrderDefaults;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.entity.Configuracion;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.service.config.BusinessConfigService;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Parámetros del negocio.
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/config")
@Tag(name = "Config", description = "Parámetros del negocio")
@RequiredArgsConstructor
public class BusinessConfigController {

    private final BusinessConfigService config;

    /**
     * Datos del negocio.
     *
     * <p>Abierto a cualquier usuario autenticado: el comprobante los necesita para
     * su cabecera y lo imprime quien despacha, no solo el dueño.
     */
    @PostMapping("/business")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<BusinessInfo>> getNegocio() {
        return new ResponseEntity<>(
                new Response<>(config.getNegocio(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/business/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<BusinessInfo>> guardarNegocio(@RequestBody BusinessInfo body) {
        return new ResponseEntity<>(
                new Response<>(config.guardarNegocio(body), new StatusCode(200, "Datos guardados")),
                HttpStatus.OK);
    }

    /** Tasa y estado del ITBMS, para que el checkout pinte el desglose. */
    @PostMapping("/itbms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<Map<String, Object>>> getItbms() {
        return new ResponseEntity<>(
                new Response<>(Map.of(
                        "tasa", config.getItbmsTasa(),
                        "activo", config.isItbmsActivo()),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/itbms/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<Map<String, Object>>> saveItbms(@RequestBody Map<String, Object> body) {

        Object tasa = body.get("tasa");
        Object activo = body.get("activo");

        if (tasa != null) {
            try {
                var valor = new java.math.BigDecimal(tasa.toString().trim());
                if (valor.signum() < 0 || valor.compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessException("La tasa debe estar entre 0 y 100", 400);
                }
                config.guardar(Configuracion.ITBMS_TASA, valor.toPlainString(), null);
            } catch (NumberFormatException e) {
                throw new BusinessException("Tasa inválida: " + tasa, 400);
            }
        }

        if (activo != null) {
            config.guardar(Configuracion.ITBMS_ACTIVO, String.valueOf(Boolean.parseBoolean(activo.toString())), null);
        }

        return new ResponseEntity<>(
                new Response<>(Map.of(
                        "tasa", config.getItbmsTasa(),
                        "activo", config.isItbmsActivo()),
                        new StatusCode(200, "Configuración actualizada")),
                HttpStatus.OK);
    }

    /** Cliente y categoría con los que arranca crear orden. */
    @PostMapping("/pos-defaults")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<OrderDefaults>> getPosDefaults() {
        return new ResponseEntity<>(
                new Response<>(config.getOrderDefaults(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/pos-defaults/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<OrderDefaults>> savePosDefaults(@RequestBody OrderDefaults body) {
        return new ResponseEntity<>(
                new Response<>(
                        config.guardarOrderDefaults(body.clienteDefectoId(), body.categoriaDefectoId()),
                        new StatusCode(200, "Configuración guardada")),
                HttpStatus.OK);
    }
}
