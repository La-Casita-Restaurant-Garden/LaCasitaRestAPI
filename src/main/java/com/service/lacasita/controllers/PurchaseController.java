package com.service.lacasita.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.service.lacasita.dto.AbonoProveedorRequest;
import com.service.lacasita.dto.CompraRequest;
import com.service.lacasita.dto.CompraResumen;
import com.service.lacasita.dto.PageResponse;
import com.service.lacasita.dto.Response;
import com.service.lacasita.dto.StatusCode;
import com.service.lacasita.entity.Compra;
import com.service.lacasita.entity.CuentaProveedor;
import com.service.lacasita.entity.MovimientoProveedor;
import com.service.lacasita.entity.Proveedor;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.purchase.PayableService;
import com.service.lacasita.service.purchase.PurchaseService;
import com.service.lacasita.service.purchase.SupplierService;

/**
 * Compras, proveedores y cuentas por pagar.
 *
 * @author Jason Nunez
 */
@RestController
@RequestMapping("/compras")
@PreAuthorize("@perm.esAdmin(authentication)")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private PayableService payableService;

    @Autowired
    private UserRespository userRepository;

    // ── Proveedores ───────────────────────────────────────────────────────

    @PostMapping("/proveedores")
    public ResponseEntity<Response<List<Proveedor>>> proveedores() {
        return ok(supplierService.listar());
    }

    /** Solo los activos: es lo que se ofrece al cargar una compra. */
    @PostMapping("/proveedores/activos")
    public ResponseEntity<Response<List<Proveedor>>> proveedoresActivos() {
        return ok(supplierService.activos());
    }

    @PostMapping("/proveedores/save")
    public ResponseEntity<Response<Proveedor>> guardarProveedor(@RequestBody Proveedor body) {
        return new ResponseEntity<>(
                new Response<>(supplierService.guardar(body), new StatusCode(200, "Proveedor guardado")),
                HttpStatus.OK);
    }

    @PostMapping("/proveedores/toggle")
    public ResponseEntity<Response<Proveedor>> alternarProveedor(@RequestBody Proveedor body) {

        Proveedor p = supplierService.alternarActivo(body.getId());

        return new ResponseEntity<>(
                new Response<>(p, new StatusCode(200, Boolean.TRUE.equals(p.getActivo())
                        ? "Proveedor activo otra vez"
                        : "Proveedor desactivado")),
                HttpStatus.OK);
    }

    // ── Compras ───────────────────────────────────────────────────────────

    @PostMapping("/list")
    public ResponseEntity<PageResponse<CompraResumen>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<CompraResumen> compras = purchaseService.listar(page, size);

        return new ResponseEntity<>(
                new PageResponse<>(compras, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/find")
    public ResponseEntity<Response<Compra>> porId(@RequestBody CompraRequest body) {
        return new ResponseEntity<>(
                new Response<>(purchaseService.porId(body.getId()), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    /** Alta o edición del borrador. No toca el inventario. */
    @PostMapping("/save")
    public ResponseEntity<Response<Compra>> guardar(@RequestBody CompraRequest body, Authentication auth) {
        return new ResponseEntity<>(
                new Response<>(purchaseService.guardar(body, userId(auth)),
                        new StatusCode(200, "Compra guardada")),
                HttpStatus.OK);
    }

    /** Mete la mercancía al inventario y, si fue a crédito, la deuda al proveedor. */
    @PostMapping("/confirm")
    public ResponseEntity<Response<Compra>> confirmar(@RequestBody CompraRequest body, Authentication auth) {
        return new ResponseEntity<>(
                new Response<>(purchaseService.confirmar(body.getId(), userId(auth)),
                        new StatusCode(200, "Compra confirmada")),
                HttpStatus.OK);
    }

    /** Deshace la compra con contrapartidas. Nada se borra. */
    @PostMapping("/void")
    public ResponseEntity<Response<Compra>> anular(@RequestBody CompraRequest body, Authentication auth) {
        return new ResponseEntity<>(
                new Response<>(purchaseService.anular(body.getId(), body.getNota(), userId(auth)),
                        new StatusCode(200, "Compra anulada")),
                HttpStatus.OK);
    }

    // ── Cuentas por pagar ─────────────────────────────────────────────────

    @PostMapping("/por-pagar")
    public ResponseEntity<Response<List<CuentaProveedor>>> porPagar() {
        return ok(payableService.conDeuda());
    }

    @PostMapping("/por-pagar/total")
    public ResponseEntity<Response<BigDecimal>> totalPorPagar() {
        return new ResponseEntity<>(
                new Response<>(payableService.totalPorPagar(), new StatusCode(200, "Successfully")),
                HttpStatus.OK);
    }

    @PostMapping("/por-pagar/movimientos")
    public ResponseEntity<PageResponse<MovimientoProveedor>> movimientos(
            @RequestBody AbonoProveedorRequest body,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<MovimientoProveedor> movimientos =
                payableService.movimientos(body.getProveedorId(), page, size);

        return new ResponseEntity<>(
                new PageResponse<>(movimientos, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    @PostMapping("/por-pagar/abonar")
    public ResponseEntity<Response<MovimientoProveedor>> abonar(
            @RequestBody AbonoProveedorRequest body, Authentication auth) {

        return new ResponseEntity<>(
                new Response<>(payableService.abonar(body, userId(auth)),
                        new StatusCode(200, "Abono registrado")),
                HttpStatus.CREATED);
    }

    // ──────────────────────────────────────────────────────────────────────

    private <T> ResponseEntity<Response<T>> ok(T cuerpo) {
        return new ResponseEntity<>(new Response<>(cuerpo, new StatusCode(200, "Successfully")), HttpStatus.OK);
    }

    /** Para dejar firmado quién hizo cada cosa. */
    private Long userId(Authentication auth) {
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).map(UserModel::getId).orElse(null);
    }
}
