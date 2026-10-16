package com.service.lacasita.controllers;

import java.math.BigDecimal;
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

import com.service.lacasita.dto.CreditRequest;
import com.service.lacasita.dto.DebtorResponse;
import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.dto.UserId;
import com.service.lacasita.entity.CreditMovement;
import com.service.lacasita.entity.CustomerAccount;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.credit.CreditService;

import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

/**
 * Crédito de clientes (fiado).
 */
@RestController
@RequestMapping("/credits")
@Tag(name = "Credits", description = "Cuentas de crédito y cobros a clientes")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;
    private final UserRespository userRepository;

    @PostMapping("/account")
    @PreAuthorize("@perm.lee(authentication, 'clients')")
    public ResponseEntity<Response<CustomerAccount>> getAccount(@RequestBody UserId body) {
        return new ResponseEntity<>(
                new Response<>(creditService.getCuenta(body.getId()).orElse(null),
                        new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/statement")
    @PreAuthorize("@perm.lee(authentication, 'clients')")
    public ResponseEntity<PageResponse<CreditMovement>> getStatement(
            @RequestBody UserId body,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<CreditMovement> movimientos = creditService.getEstadoCuenta(body.getId(), page, size);
        return new ResponseEntity<>(
                new PageResponse<>(movimientos, new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Quién debe y cuánto, de mayor a menor. */
    @PostMapping("/debtors")
    @PreAuthorize("@perm.lee(authentication, 'clients')")
    public ResponseEntity<Response<List<DebtorResponse>>> getDebtors() {
        return new ResponseEntity<>(
                new Response<>(creditService.getDeudoresDetallados(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Total por cobrar, para el encabezado de la pantalla de deudores. */
    @PostMapping("/total")
    @PreAuthorize("@perm.lee(authentication, 'clients')")
    public ResponseEntity<Response<BigDecimal>> getTotal() {
        return new ResponseEntity<>(
                new Response<>(creditService.getTotalPorCobrar(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    // ---------------------------------------------------------------------
    // Movimientos
    // ---------------------------------------------------------------------

    /** El cliente paga. Baja el saldo total, no un pedido concreto. */
    @PostMapping("/payment")
    @PreAuthorize("@perm.puede(authentication, 'clients', 'write')")
    public ResponseEntity<Response<CreditMovement>> payment(
            @RequestBody CreditRequest body, Authentication auth) {

        CreditMovement m = creditService.abonar(
                body.getCustomerId(), body.getMonto(), body.getIdPago(),
                body.getNota(), currentUserId(auth));

        return new ResponseEntity<>(new Response<>(m, new StatusCode(200, "Abono registrado")),
                HttpStatus.OK);
    }

    /** Abre la cuenta o cambia el límite. */
    @PostMapping("/account/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<CustomerAccount>> saveAccount(@RequestBody CreditRequest body) {

        CustomerAccount cuenta = creditService.guardarCuenta(
                body.getCustomerId(), body.getLimiteCredito(), body.getActivo());

        return new ResponseEntity<>(new Response<>(cuenta, new StatusCode(200, "Cuenta actualizada")),
                HttpStatus.OK);
    }

    /** Corrección o condonación. El monto puede ser negativo y el motivo es obligatorio. */
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<CreditMovement>> adjust(
            @RequestBody CreditRequest body, Authentication auth) {

        CreditMovement m = creditService.ajustar(
                body.getCustomerId(), body.getMonto(), body.getNota(), currentUserId(auth));

        return new ResponseEntity<>(new Response<>(m, new StatusCode(200, "Ajuste registrado")),
                HttpStatus.OK);
    }

    // ---------------------------------------------------------------------

    /** Deja firmado quién hizo cada movimiento. */
    private Long currentUserId(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).map(UserModel::getId).orElse(null);
    }
}
