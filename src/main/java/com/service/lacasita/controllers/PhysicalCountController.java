package com.service.lacasita.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.ConteoRequest;
import com.service.lacasita.dto.ConteoResumen;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.entity.Conteo;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.inventory.PhysicalCountService;

/**
 * Conteo físico. El ajuste que produce es la puerta trasera del inventario:
 * solo ADMIN, igual que las compras.
 *
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/conteos")
@PreAuthorize("@perm.esAdmin(authentication)")
public class PhysicalCountController {

    @Autowired
    private PhysicalCountService countService;

    @Autowired
    private UserRespository userRepository;

    @PostMapping("/abierto")
    public ResponseEntity<Response<Conteo>> abierto() {
        return ok(countService.abierto());
    }

    @PostMapping("/find")
    public ResponseEntity<Response<Conteo>> porId(@RequestBody ConteoRequest body) {
        return ok(countService.porId(body.getId()));
    }

    @PostMapping("/recientes")
    public ResponseEntity<Response<List<ConteoResumen>>> recientes() {
        return ok(countService.recientes());
    }

    @PostMapping("/abrir")
    public ResponseEntity<Response<Conteo>> abrir(@RequestBody ConteoRequest body, Authentication auth) {
        return new ResponseEntity<>(
                new Response<>(countService.abrir(body.getNota(), userId(auth)),
                        new StatusCode(200, "Conteo abierto")),
                HttpStatus.OK);
    }

    /** Guarda lo contado hasta ahora. Se puede llamar varias veces mientras esté ABIERTO. */
    @PostMapping("/lineas")
    public ResponseEntity<Response<Conteo>> guardarLineas(@RequestBody ConteoRequest body) {
        return new ResponseEntity<>(
                new Response<>(countService.guardarLineas(body.getId(), body.getLineas()),
                        new StatusCode(200, "Conteo guardado")),
                HttpStatus.OK);
    }

    @PostMapping("/cerrar")
    public ResponseEntity<Response<Conteo>> cerrar(@RequestBody ConteoRequest body, Authentication auth) {
        return new ResponseEntity<>(
                new Response<>(countService.cerrar(body.getId(), userId(auth)),
                        new StatusCode(200, "Conteo cerrado: ajustes aplicados")),
                HttpStatus.OK);
    }

    @PostMapping("/anular")
    public ResponseEntity<Response<Conteo>> anular(@RequestBody ConteoRequest body) {
        return new ResponseEntity<>(
                new Response<>(countService.anular(body.getId()),
                        new StatusCode(200, "Conteo anulado")),
                HttpStatus.OK);
    }


    private <T> ResponseEntity<Response<T>> ok(T cuerpo) {
        return new ResponseEntity<>(new Response<>(cuerpo, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    private Long userId(Authentication auth) {
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).map(UserModel::getId).orElse(null);
    }
}
