package com.service.lacasita.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.TicketResponse;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.service.orders.TicketService;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Comprobante de un pedido.
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/tickets")
@Tag(name = "Tickets", description = "Comprobantes de pedido")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/order")
    @PreAuthorize("@perm.lee(authentication, 'orders')")
    public ResponseEntity<Response<TicketResponse>> deOrden(@RequestBody UserId body) {
        return new ResponseEntity<>(
                new Response<>(ticketService.construir(body.getId()),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }
}
