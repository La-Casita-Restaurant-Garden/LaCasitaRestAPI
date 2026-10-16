package com.service.lacasita.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.entity.DiscountType;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.DiscountTypeRepository;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Catálogo de descuentos.
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/discounts")
@Tag(name = "Discounts", description = "Tipos de descuento aplicables a un pedido")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountTypeRepository repository;

    /** Los que el personal puede elegir en el checkout. */
    @PostMapping("/types")
    @PreAuthorize("@perm.lee(authentication, 'orders')")
    public ResponseEntity<Response<List<DiscountType>>> activos() {
        return new ResponseEntity<>(
                new Response<>(repository.findByActivoTrueOrderByNombreAsc(),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Todos, incluidos los desactivados, para administrarlos. */
    @PostMapping("/types/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<DiscountType>>> todos() {
        return new ResponseEntity<>(
                new Response<>(repository.findAll(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/types/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<DiscountType>> guardar(@RequestBody DiscountType body) {

        validar(body);

        DiscountType tipo = body.getId() == null
                ? DiscountType.builder().build()
                : repository.findById(body.getId()).orElseThrow(() ->
                        new BusinessException("Tipo de descuento no encontrado: " + body.getId(), 404));

        tipo.setNombre(body.getNombre().trim());
        tipo.setAlcance(body.getAlcance());
        tipo.setPorcentaje(body.getPorcentaje());
        tipo.setMontoFijo(body.getMontoFijo());
        tipo.setRequiereMotivo(Boolean.TRUE.equals(body.getRequiereMotivo()));
        tipo.setRequiereAdmin(Boolean.TRUE.equals(body.getRequiereAdmin()));
        tipo.setRequiereDocumento(Boolean.TRUE.equals(body.getRequiereDocumento()));
        tipo.setActivo(body.getActivo() == null || body.getActivo());

        return new ResponseEntity<>(
                new Response<>(repository.save(tipo), new StatusCode(200, "Descuento guardado")),
                HttpStatus.OK);
    }

    /**
     * Desactiva un tipo.
     */
    @PostMapping("/types/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<DiscountType>> alternar(@RequestBody DiscountType body) {

        DiscountType tipo = repository.findById(body.getId()).orElseThrow(() ->
                new BusinessException("Tipo de descuento no encontrado: " + body.getId(), 404));

        tipo.setActivo(!Boolean.TRUE.equals(tipo.getActivo()));

        return new ResponseEntity<>(
                new Response<>(repository.save(tipo), new StatusCode(200,
                        Boolean.TRUE.equals(tipo.getActivo()) ? "Descuento activado" : "Descuento desactivado")),
                HttpStatus.OK);
    }

    // ---------------------------------------------------------------------

    private void validar(DiscountType body) {

        if (body.getNombre() == null || body.getNombre().isBlank()) {
            throw new BusinessException("El descuento necesita un nombre", 400);
        }

        if (!DiscountType.ALCANCE_ORDEN.equals(body.getAlcance())
                && !DiscountType.ALCANCE_PRODUCTO.equals(body.getAlcance())) {
            throw new BusinessException("Alcance inválido: use ORDEN o PRODUCTO", 400);
        }

        if (body.getPorcentaje() != null && body.getMontoFijo() != null) {
            throw new BusinessException(
                    "Un descuento es de porcentaje o de monto fijo, no de los dos", 400);
        }

        if (body.getPorcentaje() != null
                && (body.getPorcentaje().signum() <= 0
                        || body.getPorcentaje().compareTo(java.math.BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessException("El porcentaje debe estar entre 0 y 100", 400);
        }

        if (body.getMontoFijo() != null && body.getMontoFijo().signum() <= 0) {
            throw new BusinessException("El monto fijo debe ser mayor que cero", 400);
        }

        // Un descuento de importe libre lo escribe quien lo aplica, así que sin
        // motivo no hay forma de defenderlo en una revisión posterior.
        if (body.getPorcentaje() == null && body.getMontoFijo() == null
                && !Boolean.TRUE.equals(body.getRequiereMotivo())) {
            throw new BusinessException(
                    "Un descuento de importe libre debe exigir motivo", 400);
        }
    }
}
