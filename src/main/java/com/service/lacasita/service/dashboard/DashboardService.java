package com.service.lacasita.service.dashboard;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.service.lacasita.dto.DashboardKpiDTO;
import com.service.lacasita.repository.SalesRepository;
import com.service.lacasita.service.orders.OrderService;
import com.service.lacasita.service.customer.CustomerService;

/**
 * Los tres números de la cabecera del panel.
 *
 * <h2>Pedidos e ingresos NO salen de la misma tabla, y es a propósito</h2>
 *
 * <ul>
 *   <li><b>Pedidos</b> cuenta lo tomado hoy, de {@code orders}. Es actividad:
 *       incluye lo que sigue en cocina.</li>
 *   <li><b>Ingresos</b> suma lo reconocido hoy, de {@code venta}. Es dinero: solo
 *       entra cuando el pedido se completa.</li>
 * </ul>
 *
 * <p>Antes los dos salían de {@code orders}, así que un pedido tomado hacía dos
 * minutos —sin cocinar y sin cobrar— ya figuraba como ingreso del día. Además de
 * ser falso, hacía que <b>el panel y el cierre de caja dieran cifras distintas del
 * mismo día</b>, porque el cierre siempre leyó de {@code venta}.
 *
 * <p>Que las dos cifras no cuadren entre sí es lo correcto y lo esperable: "hoy
 * llevamos 40 pedidos y $520 facturados" significa que hay pedidos sin despachar.
 *
 * @author Jason Nunez
 */
@Service
public class DashboardService {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final SalesRepository salesRepository;

    public DashboardService(OrderService orderService, CustomerService customerService,
            SalesRepository salesRepository) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.salesRepository = salesRepository;
    }

    @Cacheable(value = "dashboardKpis", key = "#date.toLocalDate().toString()")
    public DashboardKpiDTO getKpis(LocalDateTime date) {

        DashboardKpiDTO kpis = new DashboardKpiDTO();

        kpis.setTotalOrders(orderService.getNumOrders(date));
        kpis.setTotalCustomers(customerService.getTotalCustomers());
        kpis.setTotalRevenue(ingresosDe(date));

        return kpis;
    }

    /**
     * Lo facturado ese día: ventas reconocidas, sin las anuladas.
     *
     * <p>La ventana se cierra en el último instante del día. `sumTotalBetween` usa
     * {@code <=} en el extremo, así que se le pasa 23:59:59.999999999 y no la
     * medianoche siguiente, que contaría dos veces la primera venta del día que
     * viene.
     */
    private Double ingresosDe(LocalDateTime date) {

        LocalDateTime inicio = date.toLocalDate().atStartOfDay();

        Double total = salesRepository.sumTotalBetween(inicio, inicio.plusDays(1).minusNanos(1));

        return total != null ? total : 0d;
    }
}
