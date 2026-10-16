package com.service.lacasita.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.EmailTemplatePreview;
import com.service.lacasita.dto.EmailTemplatePreviewRequest;
import com.service.lacasita.dto.EmailTemplateVariable;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.EmailTemplate;
import com.service.lacasita.service.email.EmailTemplateService;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Plantillas de correo (cotizaciones y, a futuro, otros envíos transaccionales).
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/email-templates")
@Tag(name = "Email templates", description = "Plantillas de correo editables desde Settings")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService service;

    /** Las que el personal puede elegir al enviar una cotización. */
    @PostMapping("/active")
    @PreAuthorize("@perm.lee(authentication, 'sales')")
    public ResponseEntity<Response<List<EmailTemplate>>> activas() {
        return new ResponseEntity<>(
                new Response<>(service.listarActivas(), new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    /** Todas, incluidas las desactivadas, para administrarlas. */
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<EmailTemplate>>> todas() {
        return new ResponseEntity<>(
                new Response<>(service.listarTodas(), new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<EmailTemplate>> guardar(@RequestBody EmailTemplate body) {
        return new ResponseEntity<>(
                new Response<>(service.guardar(body), new StatusCode(200, "Plantilla guardada")), HttpStatus.OK);
    }

    /** Activa o desactiva una plantilla. */
    @PostMapping("/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<EmailTemplate>> alternar(@RequestBody UserId body) {

        EmailTemplate plantilla = service.alternar(body.getId());
        return new ResponseEntity<>(
                new Response<>(plantilla, new StatusCode(200,
                        Boolean.TRUE.equals(plantilla.getActiva()) ? "Plantilla activada" : "Plantilla desactivada")),
                HttpStatus.OK);
    }

    /** Los marcadores {{clave}} disponibles para una cotización, para el editor. */
    @PostMapping("/variables/cotizacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<EmailTemplateVariable>>> variablesCotizacion() {
        return new ResponseEntity<>(
                new Response<>(EmailTemplateService.variablesCotizacion(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Previsualiza un HTML/asunto (guardado o no) con datos de ejemplo de una cotización. */
    @PostMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<EmailTemplatePreview>> preview(@RequestBody EmailTemplatePreviewRequest body) {

        var valores = EmailTemplateService.valoresDeEjemploCotizacion();
        EmailTemplatePreview preview = new EmailTemplatePreview(
                EmailTemplateService.sustituir(body.asunto(), valores),
                EmailTemplateService.sustituir(body.html(), valores));

        return new ResponseEntity<>(new Response<>(preview, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }
}
