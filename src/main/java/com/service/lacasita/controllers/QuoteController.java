package com.service.lacasita.controllers;

import java.util.List;

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

import com.service.lacasita.dto.OrderResponse;
import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.QuoteRequest;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.QuotePayment;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.orders.OrderService;
import com.service.lacasita.service.orders.QuotePaymentService;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Acciones sobre un documento de orden ya existente (venta o cotización, según
 * su estado — ver {@code OrderService#etiquetaDocumento}): compartir su link
 * público, enviarlo/previsualizarlo por correo, y sus depósitos.
 *
 * <p>La idea de un módulo de "Cotizaciones" con su propio flujo de
 * creación/edición se descartó — ver backup en {@code _backups/} en la raíz
 * del proyecto. Lo que queda acá es solo lo que otras pantallas (Ventas,
 * Detalle de orden) todavía usan de verdad.
 *
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/orders/quotes")
@Tag(name = "Quotes", description = "Documentos de orden: correo, link y depósitos")
@RequiredArgsConstructor
@PreAuthorize("@perm.lee(authentication, 'sales')")
public class QuoteController {

    private final OrderService orderService;
    private final QuotePaymentService quotePaymentService;
    private final UserRespository userRepository;

    @PostMapping("/share-link")
    @PreAuthorize("@perm.puede(authentication, 'sales', 'write')")
    public ResponseEntity<Response<String>> shareLink(@RequestBody UserId body) {

        String link = orderService.generarLinkCotizacion(body.getId());
        return new ResponseEntity<>(new Response<>(link, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/email-preview")
    @PreAuthorize("@perm.puede(authentication, 'sales', 'write')")
    public ResponseEntity<Response<String>> emailPreview(@RequestBody QuoteRequest body) {

        String html = orderService.previsualizarCorreoCotizacion(body.getOrderId(), body.getPlantillaId());
        return new ResponseEntity<>(new Response<>(html, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/send-email")
    @PreAuthorize("@perm.puede(authentication, 'sales', 'write')")
    public ResponseEntity<Response<Boolean>> sendEmail(@RequestBody QuoteRequest body) {

        orderService.enviarCotizacionPorCorreo(body.getOrderId(), body.getPlantillaId(), body.getEmail());
        return new ResponseEntity<>(new Response<>(true, new StatusCode(200, "Correo enviado")), HttpStatus.OK);
    }

    @PostMapping("/payment")
    @PreAuthorize("@perm.puede(authentication, 'sales', 'write')")
    public ResponseEntity<Response<QuotePayment>> payment(@RequestBody QuoteRequest body, Authentication auth) {

        QuotePayment pago = quotePaymentService.registrar(
                body.getOrderId(), body.getMonto(), body.getMetodoPagoId(), body.getNota(), userId(auth));

        return new ResponseEntity<>(
                new Response<>(pago, new StatusCode(200, "Depósito registrado")), HttpStatus.OK);
    }

    @PostMapping("/payment/history")
    public ResponseEntity<Response<List<QuotePayment>>> paymentHistory(@RequestBody UserId body) {
        return new ResponseEntity<>(
                new Response<>(quotePaymentService.historial(body.getId()), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    // ---------------------------------------------------------------------

    private Long userId(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).map(UserModel::getId).orElse(null);
    }
}
