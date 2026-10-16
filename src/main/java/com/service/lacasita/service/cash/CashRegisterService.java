package com.service.lacasita.service.cash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.CashCloseSummary;
import com.service.lacasita.dto.CashCloseSummary.LineaDescuento;
import com.service.lacasita.dto.CashCloseSummary.LineaMetodo;
import com.service.lacasita.entity.CashSession;
import com.service.lacasita.entity.DiscountType;
import com.service.lacasita.entity.PaymentMethod;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.CashSessionRepository;
import com.service.lacasita.repository.CreditMovementRepository;
import com.service.lacasita.repository.DiscountTypeRepository;
import com.service.lacasita.repository.OrderDiscountRepository;
import com.service.lacasita.repository.PaymentMethodRepository;
import com.service.lacasita.repository.QuotePaymentRepository;
import com.service.lacasita.repository.SalesRepository;
import com.service.lacasita.utils.PaymentMethods;

import lombok.RequiredArgsConstructor;

/**
 * Cierre de caja.
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private static final Logger log = LoggerFactory.getLogger(CashRegisterService.class);

    /** Las ventas se fechan en hora local; los movimientos, en UTC. */
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private final CashSessionRepository sessionRepository;
    private final SalesRepository salesRepository;
    private final CreditMovementRepository movementRepository;
    private final QuotePaymentRepository quotePaymentRepository;
    private final OrderDiscountRepository discountRepository;
    private final DiscountTypeRepository discountTypeRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    // ---------------------------------------------------------------------
    // Sesión
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public CashSession getSesionAbierta() {
        return buscarAbierta().orElse(null);
    }

    /**
     * La caja abierta, si la hay.
     *
     * <p>Vive aquí y no como método {@code default} del repositorio: un default se
     * simula junto con el resto de la interfaz, de modo que las pruebas que
     * preparaban la consulta derivada veían igualmente un Optional vacío.
     */
    private Optional<CashSession> buscarAbierta() {
        return sessionRepository.findByEstadoOrderByAbiertaAtDesc(CashSession.ABIERTA)
                .stream().findFirst();
    }

    @Transactional(readOnly = true)
    public Page<CashSession> getHistorial(int page, int size) {
        return sessionRepository.findAllByOrderByAbiertaAtDesc(PageRequest.of(page, size));
    }

    /**
     * Abre el turno declarando el fondo inicial.
     *
     * @throws BusinessException 409 si ya hay una caja abierta
     */
    @Transactional
    public CashSession abrir(BigDecimal fondoInicial, Long userId) {

        if (fondoInicial == null || fondoInicial.signum() < 0) {
            throw new BusinessException("El fondo inicial no puede ser negativo", 400);
        }

        buscarAbierta().ifPresent(s -> {
            throw new BusinessException(
                    "Ya hay una caja abierta desde %s. Ciérrela antes de abrir otra."
                            .formatted(s.getAbiertaAt()), 409);
        });

        try {
            return sessionRepository.save(CashSession.builder()
                    .abiertaAt(Instant.now())
                    .fondoInicial(fondoInicial.setScale(2, RoundingMode.HALF_UP))
                    .estado(CashSession.ABIERTA)
                    .abiertaPor(userId)
                    .build());

        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Otra persona acaba de abrir la caja", 409);
        }
    }

    /**
     * Cierra el turno con lo que se contó físicamente.
     */
    @Transactional
    public CashSession cerrar(BigDecimal efectivoContado, String notas, Long userId) {

        if (efectivoContado == null || efectivoContado.signum() < 0) {
            throw new BusinessException("El efectivo contado no puede ser negativo", 400);
        }

        CashSession sesion = buscarAbierta().orElseThrow(() ->
                new BusinessException("No hay ninguna caja abierta", 409));

        Instant cierre = Instant.now();
        CashCloseSummary resumen = resumir(sesion, sesion.getAbiertaAt(), cierre);

        BigDecimal contado = efectivoContado.setScale(2, RoundingMode.HALF_UP);

        sesion.setCerradaAt(cierre);
        sesion.setEfectivoEsperado(resumen.efectivoEsperado());
        sesion.setEfectivoContado(contado);
        sesion.setDiferencia(contado.subtract(resumen.efectivoEsperado()));
        sesion.setEstado(CashSession.CERRADA);
        sesion.setCerradaPor(userId);
        sesion.setNotas(notas);

        if (sesion.getDiferencia().signum() != 0) {
            log.info("Caja {} cerrada con diferencia de {}", sesion.getId(), sesion.getDiferencia());
        }

        return sessionRepository.save(sesion);
    }

    // ---------------------------------------------------------------------
    // Resumen
    // ---------------------------------------------------------------------

    /** Estado de la caja abierta ahora mismo, sin cerrarla. */
    @Transactional(readOnly = true)
    public CashCloseSummary resumenActual() {

        CashSession sesion = buscarAbierta().orElseThrow(() ->
                new BusinessException("No hay ninguna caja abierta", 409));

        return resumir(sesion, sesion.getAbiertaAt(), Instant.now());
    }

    /** Resumen de un rango arbitrario, para consultar días ya cerrados. */
    @Transactional(readOnly = true)
    public CashCloseSummary resumenDe(Instant desde, Instant hasta) {
        return resumir(null, desde, hasta);
    }

    private CashCloseSummary resumir(CashSession sesion, Instant desde, Instant hasta) {

        LocalDateTime desdeLocal = LocalDateTime.ofInstant(desde, ZONA);
        LocalDateTime hastaLocal = LocalDateTime.ofInstant(hasta, ZONA);

        Map<Long, PaymentMethod> metodos = paymentMethodRepository.findAll().stream()
                .collect(Collectors.toMap(PaymentMethod::getIdmetodo, Function.identity()));

        // --- Ventas por método -------------------------------------------
        List<LineaMetodo> ventas = new ArrayList<>();
        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal ventasEfectivo = BigDecimal.ZERO;
        BigDecimal fiado = BigDecimal.ZERO;

        for (Object[] fila : salesRepository.resumenPorMetodo(desdeLocal, hastaLocal)) {
            Long idPago = (Long) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            BigDecimal total = aDecimal(fila[2]);

            PaymentMethod metodo = idPago == null ? null : metodos.get(idPago);
            ventas.add(new LineaMetodo(idPago, nombreDe(metodo), cantidad, total));

            totalVentas = totalVentas.add(total);
            if (PaymentMethods.esEfectivo(metodo)) ventasEfectivo = ventasEfectivo.add(total);
            if (PaymentMethods.esCredito(metodo)) fiado = fiado.add(total);
        }

        // --- Abonos de deuda ---------------------------------------------
        List<LineaMetodo> abonos = new ArrayList<>();
        BigDecimal totalAbonos = BigDecimal.ZERO;
        BigDecimal abonosEfectivo = BigDecimal.ZERO;

        for (Object[] fila : movementRepository.abonosPorMetodo(desde, hasta)) {
            Long idPago = (Long) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            BigDecimal total = aDecimal(fila[2]);

            PaymentMethod metodo = idPago == null ? null : metodos.get(idPago);
            abonos.add(new LineaMetodo(idPago, nombreDe(metodo), cantidad, total));

            totalAbonos = totalAbonos.add(total);

            if (metodo == null || PaymentMethods.esEfectivo(metodo)) {
                abonosEfectivo = abonosEfectivo.add(total);
            }
        }

        // --- Depósitos de cotización --------------------------------------
        List<LineaMetodo> depositosCotizacion = new ArrayList<>();
        BigDecimal totalDepositosCotizacion = BigDecimal.ZERO;
        BigDecimal depositosCotizacionEfectivo = BigDecimal.ZERO;

        for (Object[] fila : quotePaymentRepository.depositosPorMetodo(desde, hasta)) {
            Long idPago = (Long) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            BigDecimal total = aDecimal(fila[2]);

            PaymentMethod metodo = idPago == null ? null : metodos.get(idPago);
            depositosCotizacion.add(new LineaMetodo(idPago, nombreDe(metodo), cantidad, total));

            totalDepositosCotizacion = totalDepositosCotizacion.add(total);

            if (metodo == null || PaymentMethods.esEfectivo(metodo)) {
                depositosCotizacionEfectivo = depositosCotizacionEfectivo.add(total);
            }
        }

        // --- Descuentos ---------------------------------------------------
        List<LineaDescuento> descuentos = new ArrayList<>();
        BigDecimal totalDescuentos = BigDecimal.ZERO;

        List<Object[]> filasDescuento = discountRepository.resumenPorTipo(desde, hasta);

        if (!filasDescuento.isEmpty()) {
            Map<Long, DiscountType> tipos = discountTypeRepository.findAll().stream()
                    .collect(Collectors.toMap(DiscountType::getId, Function.identity()));

            for (Object[] fila : filasDescuento) {
                Long tipoId = (Long) fila[0];
                long cantidad = ((Number) fila[1]).longValue();
                BigDecimal total = aDecimal(fila[2]);

                DiscountType tipo = tipoId == null ? null : tipos.get(tipoId);
                descuentos.add(new LineaDescuento(tipoId,
                        tipo != null ? tipo.getNombre() : "Descuento", cantidad, total));

                totalDescuentos = totalDescuentos.add(total);
            }
        }

        // --- Subtotal e impuesto ------------------------------------------
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;

        List<Object[]> desglose = salesRepository.desgloseDelPeriodo(desdeLocal, hastaLocal);
        if (!desglose.isEmpty()) {
            Object[] fila = desglose.get(0);
            subtotal = aDecimal(fila[0]);
            impuesto = aDecimal(fila[2]);
        }

        BigDecimal fondo = sesion != null ? sesion.getFondoInicial() : BigDecimal.ZERO;
        BigDecimal esperado = fondo.add(ventasEfectivo).add(abonosEfectivo).add(depositosCotizacionEfectivo);

        return new CashCloseSummary(
                sesion != null ? sesion.getId() : null,
                desde, hasta,
                redondear(fondo),
                ventas, redondear(totalVentas), redondear(ventasEfectivo),
                redondear(fiado),
                abonos, redondear(totalAbonos), redondear(abonosEfectivo),
                depositosCotizacion, redondear(totalDepositosCotizacion), redondear(depositosCotizacionEfectivo),
                descuentos, redondear(totalDescuentos),
                redondear(subtotal), redondear(impuesto),
                redondear(esperado));
    }

    // ---------------------------------------------------------------------

    private String nombreDe(PaymentMethod metodo) {
        return metodo != null && metodo.getName() != null ? metodo.getName() : "Sin método";
    }

    private BigDecimal aDecimal(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        if (valor instanceof BigDecimal d) return d;
        return BigDecimal.valueOf(((Number) valor).doubleValue());
    }

    private BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
