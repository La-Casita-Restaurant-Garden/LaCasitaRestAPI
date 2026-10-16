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
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.QuickNote;
import com.service.lacasita.service.orders.QuickNoteService;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/quick-notes")
@Tag(name = "Quick Notes", description = "Notas rápidas para el comentario del pedido")
@RequiredArgsConstructor
public class QuickNoteController {

    private final QuickNoteService quickNoteService;


    @PostMapping("/find-all")
    @PreAuthorize("@perm.lee(authentication, 'orders')")
    public ResponseEntity<Response<List<QuickNote>>> findAll() {
        return new ResponseEntity<>(
                new Response<>(quickNoteService.getActivas(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/find-all-admin")
    @PreAuthorize("@perm.lee(authentication, 'settings')")
    public ResponseEntity<Response<List<QuickNote>>> findAllAdmin() {
        return new ResponseEntity<>(
                new Response<>(quickNoteService.getTodas(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/save")
    @PreAuthorize("@perm.puede(authentication, 'settings', 'write')")
    public ResponseEntity<Response<QuickNote>> save(@RequestBody QuickNote body) {
        return new ResponseEntity<>(
                new Response<>(quickNoteService.guardar(body), new StatusCode(200, "Nota guardada")),
                HttpStatus.OK);
    }

    /** Recibe la lista en el orden deseado y persiste ese orden. */
    @PostMapping("/reorder")
    @PreAuthorize("@perm.puede(authentication, 'settings', 'write')")
    public ResponseEntity<Response<String>> reorder(@RequestBody List<QuickNote> body) {
        quickNoteService.reordenar(body);
        return new ResponseEntity<>(
                new Response<>("Orden actualizado", new StatusCode(200, "OK")),
                HttpStatus.OK);
    }

    @PostMapping("/delete")
    @PreAuthorize("@perm.puede(authentication, 'settings', 'delete')")
    public ResponseEntity<Response<String>> delete(@RequestBody UserId body) {
        quickNoteService.eliminar(body.getId());
        return new ResponseEntity<>(
                new Response<>("Nota eliminada", new StatusCode(200, "OK")),
                HttpStatus.OK);
    }
}
