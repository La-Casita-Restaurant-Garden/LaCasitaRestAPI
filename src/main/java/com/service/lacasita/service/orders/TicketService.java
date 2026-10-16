package com.service.lacasita.service.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.TicketResponse;
import com.service.lacasita.dto.TicketResponse.Descuento;
import com.service.lacasita.dto.TicketResponse.Linea;
import com.service.lacasita.entity.Customer;
import com.service.lacasita.entity.DiscountType;
import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.OrderDiscount;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.DiscountTypeRepository;
import com.service.lacasita.repository.OrderDiscountRepository;
import com.service.lacasita.repository.OrderRepository;
import com.service.lacasita.service.credit.CreditService;
import com.service.lacasita.utils.PaymentMethods;

import lombok.RequiredArgsConstructor;

/**
 * Arma el comprobante de un pedido.
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final OrderRepository orderRepository;
    private final OrderDiscountRepository discountRepository;
    private final DiscountTypeRepository discountTypeRepository;
    private final CreditService creditService;
    private final QuotePaymentService quotePaymentService;

    @Transactional(readOnly = true)
    public TicketResponse construir(Long orderId) {


        Order order = orderRepository.findByIdWithItems(orderId).orElseThrow(() ->
                new BusinessException("Orden no encontrada: " + orderId, 404));

        boolean esCredito = PaymentMethods.esCredito(order.getPaymentMethod());

        return new TicketResponse(
                order.getOrderId(),
                order.getOrder_number(),
                order.getDate_placed(),
                order.getStatus(),
                nombreDe(order.getCustomer()),
                order.getCustomer() != null ? order.getCustomer().getPhone() : null,
                lineasDe(order),
                aDecimal(order.getSubtotal()),
                descuentosDe(orderId),
                aDecimal(order.getDescuento()),
                aDecimal(order.getImpuesto()),
                aDecimal(order.getEnvio()),
                aDecimal(order.getTotal()),
                totalAbonado(orderId),
                order.getPaymentMethod() != null ? order.getPaymentMethod().getName() : null,
                esCredito,
                esCredito ? saldoDe(order.getCustomer()) : null,
                aDecimalOrNull(order.getCashReceived()),
                aDecimalOrNull(order.getChangeGiven()));
    }

    // ---------------------------------------------------------------------

    private List<Linea> lineasDe(Order order) {

        if (order.getItems() == null) return List.of();

        return order.getItems().stream().map(i -> {
            BigDecimal precio = aDecimal(i.getPrice());
            BigDecimal cantidad = BigDecimal.valueOf(i.getQuantity() != null ? i.getQuantity() : 0);
            BigDecimal descuento = aDecimal(i.getDescuento());

            return new Linea(
                    i.getName(),
                    i.getQuantity(),
                    precio,
                    descuento,
                    precio.multiply(cantidad).subtract(descuento));
        }).toList();
    }

    /**
     * Descuentos aplicados, con su nombre.
     *
     * <p>El nombre se resuelve del catálogo pero el <b>porcentaje y el importe salen
     * de la fila del pedido</b>: son los que se aplicaron ese día, no los vigentes
     * hoy.
     */
    private List<Descuento> descuentosDe(Long orderId) {

        List<OrderDiscount> aplicados = discountRepository.findByOrderId(orderId);
        if (aplicados.isEmpty()) return List.of();

        Map<Long, DiscountType> tipos = discountTypeRepository.findAll().stream()
                .collect(Collectors.toMap(DiscountType::getId, Function.identity()));

        return aplicados.stream().map(d -> {
            DiscountType tipo = d.getTipoDescuentoId() == null ? null : tipos.get(d.getTipoDescuentoId());
            return new Descuento(
                    tipo != null ? tipo.getNombre() : "Descuento",
                    d.getPorcentaje(),
                    d.getMonto(),
                    d.getDocumento());
        }).toList();
    }

    /**
     * Lo abonado antes de confirmarse, si esta orden nació como cotización.
     *
     * <p>El total no se toca —sigue siendo el completo—, esto es solo para que el
     * comprobante avise cuánto ya se cobró y cuánto falta, en vez de dejar que se
     * cobre dos veces sin que nadie lo note.
     */
    private BigDecimal totalAbonado(Long orderId) {
        return quotePaymentService.historial(orderId).stream()
                .map(p -> p.getMonto() == null ? BigDecimal.ZERO : p.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Lo que el cliente debe en total, no solo por este pedido. */
    private BigDecimal saldoDe(Customer customer) {
        if (customer == null) return null;
        return creditService.getCuenta(customer.getCustomer_id())
                .map(c -> c.getSaldo())
                .orElse(null);
    }

    private String nombreDe(Customer c) {
        if (c == null) return null;
        String apellido = c.getLast_name();
        return apellido == null || apellido.isBlank() ? c.getName() : c.getName() + " " + apellido;
    }

    private BigDecimal aDecimal(Double valor) {
        return valor == null ? BigDecimal.ZERO : BigDecimal.valueOf(valor);
    }

    /** A diferencia de {@link #aDecimal}, nulo se queda nulo: no aplica, no es cero. */
    private BigDecimal aDecimalOrNull(Double valor) {
        return valor == null ? null : BigDecimal.valueOf(valor);
    }
}
