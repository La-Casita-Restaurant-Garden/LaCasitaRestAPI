package com.service.lacasita.controllers;

import java.math.BigDecimal;
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
import com.service.lacasita.entity.DeliveryType;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.DeliveryTypeRepository;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Catálogo de tipos de delivery.
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/delivery")
@Tag(name = "Delivery", description = "Tipos de delivery cobrables en un pedido")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryTypeRepository repository;

    /** Los que el personal puede elegir en el checkout. */
    @PostMapping("/types")
    @PreAuthorize("@perm.lee(authentication, 'orders')")
    public ResponseEntity<Response<List<DeliveryType>>> activos() {
        return new ResponseEntity<>(
                new Response<>(repository.findByActivoTrueOrderByNombreAsc(),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Todos, incluidos los desactivados, para administrarlos. */
    @PostMapping("/types/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<DeliveryType>>> todos() {
        return new ResponseEntity<>(
                new Response<>(repository.findAll(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/types/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<DeliveryType>> guardar(@RequestBody DeliveryType body) {

        validar(body);

        DeliveryType tipo = body.getId() == null
                ? DeliveryType.builder().build()
                : repository.findById(body.getId()).orElseThrow(() ->
                        new BusinessException("Tipo de delivery no encontrado: " + body.getId(), 404));

        tipo.setNombre(body.getNombre().trim());
        tipo.setMonto(body.getMonto());
        tipo.setActivo(body.getActivo() == null || body.getActivo());

        return new ResponseEntity<>(
                new Response<>(repository.save(tipo), new StatusCode(200, "Delivery guardado")),
                HttpStatus.OK);
    }

    /** Activa o desactiva un tipo. */
    @PostMapping("/types/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<DeliveryType>> alternar(@RequestBody DeliveryType body) {

        DeliveryType tipo = repository.findById(body.getId()).orElseThrow(() ->
                new BusinessException("Tipo de delivery no encontrado: " + body.getId(), 404));

        tipo.setActivo(!Boolean.TRUE.equals(tipo.getActivo()));

        return new ResponseEntity<>(
                new Response<>(repository.save(tipo), new StatusCode(200,
                        Boolean.TRUE.equals(tipo.getActivo()) ? "Delivery activado" : "Delivery desactivado")),
                HttpStatus.OK);
    }

    // ---------------------------------------------------------------------

    private void validar(DeliveryType body) {

        if (body.getNombre() == null || body.getNombre().isBlank()) {
            throw new BusinessException("El tipo de delivery necesita un nombre", 400);
        }

        if (body.getMonto() == null || body.getMonto().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El monto debe ser cero o mayor", 400);
        }
    }
}
