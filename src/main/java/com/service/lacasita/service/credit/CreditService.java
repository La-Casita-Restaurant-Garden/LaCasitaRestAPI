package com.service.lacasita.service.credit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.DebtorResponse;
import com.service.lacasita.entity.CreditMovement;
import com.service.lacasita.entity.Customer;
import com.service.lacasita.entity.CustomerAccount;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.CreditMovementRepository;
import com.service.lacasita.repository.CustomerAccountRepository;
import com.service.lacasita.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

/**
 * Crédito de clientes (fiado).
 *
 * <p>Regla central, la misma del inventario: <b>el saldo nunca se escribe con un
 * valor calculado en Java</b>. Toda variación pasa por un UPDATE condicional
 * atómico y deja su línea en el libro ({@code movimiento_credito}).
 *
 * <p>Modelo de saldo corriente: los abonos bajan la deuda total, no se asignan a
 * pedidos concretos. Es como funciona el fiado de mostrador y admite pagos
 * parciales sin más maquinaria. El detalle de qué se consumió se conserva porque
 * cada cargo guarda su {@code order_id}.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class CreditService {

    private static final Logger log = LoggerFactory.getLogger(CreditService.class);

    private final CustomerAccountRepository accountRepository;
    private final CreditMovementRepository movementRepository;
    private final CustomerRepository customerRepository;

    // ---------------------------------------------------------------------
    // Consulta
    // ---------------------------------------------------------------------

    /** Cuenta del cliente, o vacío si no tiene crédito habilitado. */
    @Transactional(readOnly = true)
    public Optional<CustomerAccount> getCuenta(Long customerId) {
        return accountRepository.findById(customerId);
    }

    @Transactional(readOnly = true)
    public Page<CreditMovement> getEstadoCuenta(Long customerId, int page, int size) {
        return movementRepository.findByCustomerIdOrderByCreatedAtDesc(
                customerId, PageRequest.of(page, size));
    }

    /** Clientes que deben, de mayor a menor. */
    @Transactional(readOnly = true)
    public List<CustomerAccount> getDeudores() {
        return accountRepository.findDeudores();
    }

    /**
     * Deudores con los datos que la pantalla necesita, resueltos en el servidor.
     */
    @Transactional(readOnly = true)
    public List<DebtorResponse> getDeudoresDetallados() {

        List<CustomerAccount> cuentas = accountRepository.findDeudores();
        if (cuentas.isEmpty()) return List.of();

        Map<Long, Customer> clientes = customerRepository
                .findAllById(cuentas.stream().map(CustomerAccount::getCustomerId).toList())
                .stream()
                .collect(Collectors.toMap(Customer::getCustomer_id, c -> c));

        return cuentas.stream().map(cuenta -> {
            Customer c = clientes.get(cuenta.getCustomerId());
            return new DebtorResponse(
                    cuenta.getCustomerId(),
                    c == null ? "Cliente " + cuenta.getCustomerId()
                            : (c.getName() + " " + (c.getLast_name() != null ? c.getLast_name() : "")).trim(),
                    c != null ? c.getPhone() : null,
                    cuenta.getSaldo(),
                    cuenta.getLimiteCredito(),
                    cuenta.getDisponible(),
                    cuenta.getActivo(),
                    movementRepository.fechaCargoMasAntiguo(cuenta.getCustomerId())
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPorCobrar() {
        return accountRepository.totalPorCobrar();
    }

    // ---------------------------------------------------------------------
    // Administración de la cuenta (solo ADMIN)
    // ---------------------------------------------------------------------

    /**
     * Abre la cuenta o cambia el límite.
     */
    @Transactional
    public CustomerAccount guardarCuenta(Long customerId, BigDecimal limite, Boolean activo) {

        if (limite == null || limite.signum() < 0) {
            throw new BusinessException("El límite de crédito no puede ser negativo", 400);
        }

        Customer cliente = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Cliente no encontrado: " + customerId, 404));

        CustomerAccount cuenta = accountRepository.findById(customerId).orElseGet(() ->
                CustomerAccount.builder()
                        .customerId(cliente.getCustomer_id())
                        .saldo(BigDecimal.ZERO)
                        .limiteCredito(BigDecimal.ZERO)
                        .activo(true)
                        .build());

        if (cuenta.getSaldo() != null && limite.compareTo(cuenta.getSaldo()) < 0) {
            throw new BusinessException(
                    "El límite no puede ser menor que la deuda actual (%s). Cobre primero."
                            .formatted(cuenta.getSaldo()), 409);
        }

        cuenta.setLimiteCredito(limite);
        if (activo != null) cuenta.setActivo(activo);

        return accountRepository.save(cuenta);
    }

    // ---------------------------------------------------------------------
    // Movimientos
    // ---------------------------------------------------------------------

    /**
     * Carga un consumo a crédito.
     */
    @Transactional
    public CreditMovement cargar(Long customerId, BigDecimal monto, Long orderId,
            String nota, Long userId) {

        exigirMontoPositivo(monto);

        CustomerAccount cuenta = accountRepository.findById(customerId).orElseThrow(() ->
                new BusinessException("Este cliente no tiene crédito habilitado", 409));

        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new BusinessException("El crédito de este cliente está suspendido", 409);
        }

        // Reintento del mismo pedido: se devuelve el cargo existente en vez de
        // duplicar la deuda. El índice único de la BD es la red de seguridad.
        if (orderId != null) {
            Optional<CreditMovement> yaCargado = movementRepository.findCargoDeOrden(orderId);
            if (yaCargado.isPresent()) return yaCargado.get();
        }

        // El límite se valida dentro del UPDATE: cero filas = no cabe.
        if (accountRepository.cargar(customerId, monto) == 0) {
            throw new BusinessException(
                    "Supera el límite de crédito: debe %s de un límite de %s (disponible %s)"
                            .formatted(cuenta.getSaldo(), cuenta.getLimiteCredito(),
                                    cuenta.getDisponible()), 409);
        }

        return registrar(customerId, CreditMovement.TIPO_CARGO, monto, orderId, null, nota, userId);
    }

    /** Registra un pago del cliente. Baja el saldo total, no un pedido concreto. */
    @Transactional
    public CreditMovement abonar(Long customerId, BigDecimal monto, Long idPago,
            String nota, Long userId) {

        exigirMontoPositivo(monto);

        CustomerAccount cuenta = accountRepository.findById(customerId).orElseThrow(() ->
                new BusinessException("Este cliente no tiene cuenta de crédito", 404));

        if (accountRepository.abonar(customerId, monto) == 0) {
            throw new BusinessException(
                    "El abono (%s) supera la deuda actual (%s)".formatted(monto, cuenta.getSaldo()),
                    409);
        }

        return registrar(customerId, CreditMovement.TIPO_ABONO, monto, null, idPago, nota, userId);
    }

    /**
     * Corrección o condonación. El monto puede ser negativo (perdonar deuda).
     *
     * <p>No valida el límite: es la vía deliberada para dejar una cuenta como haga
     * falta. Sí impide dejar el saldo negativo.
     */
    @Transactional
    public CreditMovement ajustar(Long customerId, BigDecimal monto, String nota, Long userId) {

        if (monto == null || monto.signum() == 0) {
            throw new BusinessException("El ajuste no puede ser cero", 400);
        }
        if (nota == null || nota.isBlank()) {
            // Un ajuste sin motivo es indefendible en una revisión posterior.
            throw new BusinessException("Todo ajuste requiere un motivo", 400);
        }

        if (accountRepository.ajustar(customerId, monto) == 0) {
            throw new BusinessException("El ajuste dejaría el saldo en negativo", 409);
        }

        return registrar(customerId, CreditMovement.TIPO_AJUSTE, monto, null, null, nota, userId);
    }

    /**
     * Revierte el cargo de un pedido cancelado.
     *
     * <p>No borra el cargo: registra un AJUSTE en contra. El libro conserva que se
     * fió y que después se anuló, que es lo que hay que poder explicar.
     *
     * @return true si había algo que revertir
     */
    @Transactional
    public boolean revertirCargoDeOrden(Long orderId, String motivo, Long userId) {

        Optional<CreditMovement> cargo = movementRepository.findCargoDeOrden(orderId);
        if (cargo.isEmpty()) return false;

        BigDecimal viva = deudaVivaDeOrden(orderId, cargo);
        if (viva.signum() <= 0) return false;   // ya estaba revertido

        aplicarDiferenciaDeOrden(cargo.get().getCustomerId(), viva.negate(), orderId, motivo, userId);
        return true;
    }

    /**
     * Deja la deuda de un pedido igual a lo que debería ser ahora mismo.
     */
    @Transactional
    public void sincronizarCargoDeOrden(Long orderId, Long customerId, BigDecimal total,
            boolean debeEstarCargado, String referencia, Long userId) {

        Optional<CreditMovement> cargo = movementRepository.findCargoDeOrden(orderId);

        BigDecimal viva = deudaVivaDeOrden(orderId, cargo);
        BigDecimal objetivo = debeEstarCargado && total != null ? total : BigDecimal.ZERO;
        BigDecimal diferencia = objetivo.subtract(viva);

        if (diferencia.signum() == 0) return;

        // Primera vez que este pedido se fía: es un CARGO en toda regla, y cargar()
        // valida el límite. El índice único de movimiento_credito impide un segundo
        // CARGO por pedido, así que a partir de aquí todo cambio es un AJUSTE.
        if (cargo.isEmpty()) {
            cargar(customerId, objetivo, orderId, referencia, userId);
            return;
        }

        aplicarDiferenciaDeOrden(cargo.get().getCustomerId(), diferencia, orderId, referencia, userId);
    }

    // ---------------------------------------------------------------------

    /**
     * Cuánto debe el cliente por este pedido ahora mismo: el cargo original más
     * todos los ajustes que se le hayan hecho.
     */
    private BigDecimal deudaVivaDeOrden(Long orderId, Optional<CreditMovement> cargo) {

        if (cargo.isEmpty()) return BigDecimal.ZERO;

        BigDecimal ajustes = movementRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .filter(m -> CreditMovement.TIPO_AJUSTE.equals(m.getTipo()))
                .map(CreditMovement::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return cargo.get().getMonto().add(ajustes);
    }

    /**
     * Mueve el saldo por un cambio en un pedido y lo registra como AJUSTE.
     *
     * <p>No usa {@link #ajustar} porque aquel es la vía deliberada del ADMIN para
     * condonar y <b>no valida el límite</b>. Aquí sí hace falta: subir el total de
     * un pedido fiado es consumo nuevo, y colarlo por encima del techo del cliente
     * a través de una edición vaciaría de sentido el control del alta.
     */
    private CreditMovement aplicarDiferenciaDeOrden(Long customerId, BigDecimal diferencia,
            Long orderId, String motivo, Long userId) {

        boolean sube = diferencia.signum() > 0;

        // Sube: mismo UPDATE condicional que un cargo, con el límite dentro.
        // Baja: solo hay que impedir que el saldo quede negativo.
        int filas = sube
                ? accountRepository.cargar(customerId, diferencia)
                : accountRepository.ajustar(customerId, diferencia);

        if (filas == 0) {
            // La cuenta solo se relee para redactar el motivo del rechazo; en el
            // camino normal el UPDATE condicional se basta solo.
            if (!sube) throw new BusinessException("El ajuste dejaría el saldo en negativo", 409);

            CustomerAccount cuenta = accountRepository.findById(customerId).orElseThrow(() ->
                    new BusinessException("Este cliente no tiene crédito habilitado", 409));

            throw new BusinessException(
                    "Supera el límite de crédito: debe %s de un límite de %s (disponible %s)"
                            .formatted(cuenta.getSaldo(), cuenta.getLimiteCredito(),
                                    cuenta.getDisponible()), 409);
        }

        return registrar(customerId, CreditMovement.TIPO_AJUSTE, diferencia, orderId, null, motivo, userId);
    }

    // ---------------------------------------------------------------------

    private CreditMovement registrar(Long customerId, String tipo, BigDecimal monto,
            Long orderId, Long idPago, String nota, Long userId) {

        // Relectura tras el UPDATE (los @Modifying limpian el contexto) para dejar
        // el saldo resultante en la línea de auditoría.
        BigDecimal saldoDespues = accountRepository.findById(customerId)
                .map(CustomerAccount::getSaldo)
                .orElse(null);

        return movementRepository.save(CreditMovement.builder()
                .customerId(customerId)
                .tipo(tipo)
                .monto(monto)
                .saldoDespues(saldoDespues)
                .orderId(orderId)
                .idPago(idPago)
                .nota(nota)
                .createdBy(userId)
                .build());
    }

    private void exigirMontoPositivo(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new BusinessException("El monto debe ser mayor que cero", 400);
        }
    }
}
