package com.service.lacasita.service.purchase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.AbonoProveedorRequest;
import com.service.lacasita.entity.CuentaProveedor;
import com.service.lacasita.entity.MovimientoProveedor;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.CuentaProveedorRepository;
import com.service.lacasita.repository.MovimientoProveedorRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PayableService {

    private final CuentaProveedorRepository cuentaRepository;
    private final MovimientoProveedorRepository movimientoRepository;

    /**
     * La cuenta del proveedor, creándola si es su primera compra a crédito.
     */
    @Transactional
    public CuentaProveedor cuentaDe(Long proveedorId) {

        return cuentaRepository.findById(proveedorId).orElseGet(() -> {
            CuentaProveedor nueva = new CuentaProveedor();
            nueva.setProveedorId(proveedorId);
            nueva.setSaldo(BigDecimal.ZERO);
            return cuentaRepository.save(nueva);
        });
    }

    /** Deuda contraída por una compra a crédito. */
    @Transactional
    public MovimientoProveedor cargar(Long proveedorId, BigDecimal monto, Long compraId,
            String nota, Long userId) {

        if (monto == null || monto.signum() <= 0) {
            throw new BusinessException("El cargo tiene que ser mayor que cero", 400);
        }

        if (compraId != null && movimientoRepository.yaTieneCargo(compraId)) {
            throw new BusinessException("Esta compra ya generó su cargo", 409);
        }

        cuentaDe(proveedorId);
        cuentaRepository.cargar(proveedorId, monto);

        return registrar(proveedorId, MovimientoProveedor.CARGO, monto, compraId, null, nota, userId);
    }

    /** Pago al proveedor. */
    @Transactional
    public MovimientoProveedor abonar(AbonoProveedorRequest req, Long userId) {

        if (req == null || req.getProveedorId() == null) {
            throw new BusinessException("Falta el proveedor", 400);
        }

        BigDecimal monto = req.getMonto();

        if (monto == null || monto.signum() <= 0) {
            throw new BusinessException("El abono tiene que ser mayor que cero", 400);
        }

        CuentaProveedor cuenta = cuentaDe(req.getProveedorId());

        if (cuentaRepository.abonar(req.getProveedorId(), monto) == 0) {
            throw new BusinessException(
                    "El abono supera lo que se debe: saldo %s".formatted(cuenta.getSaldo()), 409);
        }

        return registrar(req.getProveedorId(), MovimientoProveedor.ABONO, monto,
                null, req.getIdpago(), req.getNota(), userId);
    }

    /**
     * Corrección de saldo. Positivo sube la deuda, negativo la baja.
     *
     * <p>Es por donde se registra la anulación de una compra a crédito: el cargo
     * no se borra, se contrarresta.
     */
    @Transactional
    public MovimientoProveedor ajustar(Long proveedorId, BigDecimal monto, Long compraId,
            String nota, Long userId) {

        if (monto == null || monto.signum() == 0) {
            throw new BusinessException("El ajuste no puede ser cero", 400);
        }

        CuentaProveedor cuenta = cuentaDe(proveedorId);

        if (monto.signum() > 0) {
            cuentaRepository.cargar(proveedorId, monto);
        } else if (cuentaRepository.abonar(proveedorId, monto.negate()) == 0) {
            throw new BusinessException(
                    "El ajuste dejaría la cuenta en negativo: saldo %s".formatted(cuenta.getSaldo()), 409);
        }

        return registrar(proveedorId, MovimientoProveedor.AJUSTE, monto, compraId, null, nota, userId);
    }

    @Transactional(readOnly = true)
    public List<CuentaProveedor> conDeuda() {
        return cuentaRepository.findConDeuda();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPorPagar() {
        return cuentaRepository.totalPorPagar();
    }

    @Transactional(readOnly = true)
    public Page<MovimientoProveedor> movimientos(Long proveedorId, int page, int size) {
        return movimientoRepository.findPorProveedor(proveedorId, PageRequest.of(page, size));
    }

    private MovimientoProveedor registrar(Long proveedorId, String tipo, BigDecimal monto,
            Long compraId, Long idpago, String nota, Long userId) {

        BigDecimal saldo = cuentaRepository.findById(proveedorId)
                .map(CuentaProveedor::getSaldo)
                .orElse(BigDecimal.ZERO);

        return movimientoRepository.save(MovimientoProveedor.builder()
                .proveedorId(proveedorId)
                .tipo(tipo)
                .monto(monto)
                .saldoDespues(saldo)
                .compraId(compraId)
                .idpago(idpago)
                .nota(nota)
                .createdBy(userId)
                .build());
    }
}
