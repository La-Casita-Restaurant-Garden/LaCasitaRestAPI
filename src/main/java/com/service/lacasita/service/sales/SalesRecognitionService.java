package com.service.lacasita.service.sales;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.Sales;
import com.service.lacasita.repository.OrderRepository;
import com.service.lacasita.repository.SalesRepository;

import lombok.RequiredArgsConstructor;

/**
 * Convierte una orden completada en una venta.
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class SalesRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(SalesRecognitionService.class);
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final SalesRepository salesRepository;
    private final OrderRepository orderRepository;

    /**
     * Registra la venta — o revive la que había, si esta orden ya se había
     * devuelto y se vuelve a completar.
     *
     * <p>{@code venta.order_id} es único (una orden, a lo sumo una venta), así
     * que no se puede insertar una fila nueva para una orden que ya tuvo una
     * —aunque esté {@code CANCELLED}—: hay que reactivar esa misma fila. Sin
     * esto, devolver una venta y volver a confirmarla/cobrarla la dejaba sin
     * registrar: {@code existsByOrder_OrderId} veía la fila cancelada y
     * asumía, equivocado, que ya no había nada que hacer.
     */
    @Transactional
    @CacheEvict(value = { "salesChart", "salesChartDaily", "dashboardCharts", "dashboardKpis" }, allEntries = true)
    public boolean registrarVenta(Long orderId) {

        if (orderId == null) return false;

        Optional<Sales> existente = salesRepository.findByOrder_OrderId(orderId);

        if (existente.isPresent() && !STATUS_CANCELLED.equalsIgnoreCase(existente.get().getStatus())) {
            log.debug("La orden {} ya tiene venta registrada; se omite", orderId);
            return false;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("No se pudo registrar la venta: la orden {} no existe", orderId);
            return false;
        }

        Sales venta = existente.orElseGet(Sales::new);
        venta.setOrder(order);
        venta.setCustomer(order.getCustomer());
        venta.setStatus(STATUS_COMPLETED);
        venta.setTotal(order.getTotal() != null ? order.getTotal() : 0d);
        venta.setIdpago(order.getPaymentMethod() != null ? order.getPaymentMethod().getIdmetodo() : null);
        venta.setFecha(order.getDate_placed() != null ? order.getDate_placed() : LocalDateTime.now());

        salesRepository.save(venta);
        return true;
    }

    /**
     * Anula la venta de una orden que se cancela después de haberse completado.
     */
    @Transactional
    @CacheEvict(value = { "salesChart", "salesChartDaily", "dashboardCharts", "dashboardKpis" }, allEntries = true)
    public boolean anularVenta(Long orderId) {

        if (orderId == null) return false;

        Optional<Sales> venta = salesRepository.findByOrder_OrderId(orderId);

        if (venta.isEmpty() || STATUS_CANCELLED.equalsIgnoreCase(venta.get().getStatus())) {
            return false;
        }

        venta.get().setStatus(STATUS_CANCELLED);
        salesRepository.save(venta.get());
        return true;
    }

    /**
     * Pone al día una venta ya registrada con el estado actual de su orden.
     */
    @Transactional
    @CacheEvict(value = { "salesChart", "salesChartDaily", "dashboardCharts", "dashboardKpis" }, allEntries = true)
    public void sincronizarConOrden(Order order) {

        if (order == null || order.getOrderId() == null) return;

        salesRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(venta -> {

            Double total = order.getTotal();
            Long idPago = order.getPaymentMethod() != null
                    ? order.getPaymentMethod().getIdmetodo()
                    : null;

            boolean cambiaTotal = total != null
                    && (venta.getTotal() == null || venta.getTotal().doubleValue() != total.doubleValue());


            boolean cambiaPago = idPago != null && !idPago.equals(venta.getIdpago());

            if (!cambiaTotal && !cambiaPago) return;

            if (cambiaTotal) {
                log.info("Venta de la orden {}: importe {} -> {}",
                        order.getOrderId(), venta.getTotal(), total);
                venta.setTotal(total);
            }

            if (cambiaPago) {
                log.info("Venta de la orden {}: método de pago {} -> {}",
                        order.getOrderId(), venta.getIdpago(), idPago);
                venta.setIdpago(idPago);
            }

            salesRepository.save(venta);
        });
    }
}
