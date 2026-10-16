package com.service.lacasita.service.orders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.service.lacasita.dto.DiscountRequest;
import com.service.lacasita.dto.OrderResponse;
import com.service.lacasita.dto.OrderSummary;
import com.service.lacasita.dto.OrderCustomerResp;
import com.service.lacasita.dto.KitchenResponse;
import com.service.lacasita.entity.Customer;
import com.service.lacasita.entity.EmailTemplate;
import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.OrderItem;
import com.service.lacasita.entity.PaymentMethod;
import com.service.lacasita.entity.UserModel;
import com.service.lacasita.event.OrderCompletedEvent;
import com.service.lacasita.event.OrderCreatedEvent;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.OrderRepository;
import com.service.lacasita.repository.OrderDiscountRepository;
import com.service.lacasita.repository.OrderItemRepository;
import com.service.lacasita.repository.PaymentMethodRepository;
import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.service.credit.CreditService;
import com.service.lacasita.service.config.BusinessConfigService;
import com.service.lacasita.service.customer.CustomerService;
import com.service.lacasita.service.email.EmailTemplateService;
import com.service.lacasita.service.email.ResendEmailService;
import com.service.lacasita.service.sales.SalesRecognitionService;
import com.service.lacasita.utils.PaymentMethods;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.math.BigDecimal;

@Service
public class OrderService {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DRAFT = "DRAFT";

    private static final String KITCHEN_PENDING = "PENDING";
    private static final String KITCHEN_DRAFT = "DRAFT";

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private PaymentMethodRepository pMthRep;
    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private CreditService creditService;
    @Autowired
    private OrderPricing orderPricing;
    @Autowired
    private SalesRecognitionService salesRecognitionService;
    @Autowired
    private OrderDiscountRepository orderDiscountRepository;
    @Autowired
    private UserRespository userRepository;
    @Autowired
    private ResendEmailService resendEmailService;
    @Autowired
    private EmailTemplateService emailTemplateService;
    @Autowired
    private BusinessConfigService businessConfigService;

    @Value("${app.public-site.base-url:https://lacasitarest.com}")
    private String publicSiteBaseUrl;

    @Value("${app.mail.restaurant-name:La Casita Restaurant & Garden}")
    private String restaurantName;

    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return orderRepository.findAllOrderByDatePlacedDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getAllOrderItemsById(Long orderId) {
        return orderItemRepository.findByOrder_OrderId(orderId);
    }

    /** Cuántas órdenes del día se devuelven como mucho. */
    private static final int MAX_ORDENES_DEL_DIA = 60;

    @Transactional(readOnly = true)
    public List<OrderSummary> getOrdenesDelDia() {

        LocalDateTime inicio = LocalDate.now().atStartOfDay();

        return orderRepository.findResumenEntre(
                inicio, inicio.plusDays(1), PageRequest.of(0, MAX_ORDENES_DEL_DIA));
    }

    /*
     * Metodo para obtener todas las ordenes de un cliente
     */
    @Transactional(readOnly = true)
    public Page<OrderCustomerResp> getAllOrdersByCustomerId(Long id, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("date_placed").descending());

        Page<Order> ordersPage = orderRepository.findByCustomerId(id, pageable);

        return ordersPage.map(order -> new OrderCustomerResp(
                order.getOrderId(),
                order.getOrder_number(),
                customerFullName(order.getCustomer()),
                order.getStatus(),
                order.getPaymentMethod() != null ? order.getPaymentMethod().getName() : null,
                order.getItems() != null ? order.getItems().size() : 0,
                order.getDate_placed(),
                order.getTotal(),
                nombreDe(order.getCreatedBy())));

    }


    @Transactional(readOnly = true)
    public List<KitchenResponse> getComandasDelDia(String kitchenStatus) {

        LocalDateTime inicio = LocalDate.now().atStartOfDay();

        return orderRepository.findByKitchenStatusEntre(kitchenStatus, inicio, inicio.plusDays(1))
                .stream()
                .limit(MAX_ORDENES_DEL_DIA)
                .map(this::toKitchenResponse)
                .toList();
    }

    /**
     * Mueve una comanda en la cola de cocina.
     */
    @Transactional
    public KitchenResponse updateKitchenOrderStatusById(KitchenResponse o) {

        Order order = orderRepository.findByIdWithItems(o.getOrderId())
                .orElseThrow(() -> new BusinessException("Orden no encontrada: " + o.getOrderId(), 404));

        order.setKitchenStatus(o.getKitchenStatus());
        orderRepository.save(order);

        return toKitchenResponse(order);
    }

    /*
     * Metodo para obtener una orden por id
     */
    @Transactional(readOnly = true)
    public Order getOrdersById(Long id) {

        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException("Orden no encontrada: " + id, 404));
    }


    @Transactional
    @CacheEvict(value = { "salesChart", "salesChartDaily", "dashboardCharts", "dashboardKpis" }, allEntries = true)
    public Order createOrder(OrderResponse orderResponse, String origin, Long createdByUserId) {


        if (orderResponse.getIdempotencyKey() != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(orderResponse.getIdempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Customer customer = customerService.getCustomerById(orderResponse.getCustomer_id())
                .orElseThrow(() -> new BusinessException(
                        "Cliente no encontrado: " + orderResponse.getCustomer_id(), 404));

        if (orderResponse.getItems() == null || orderResponse.getItems().isEmpty()) {
            throw new BusinessException("La orden debe tener al menos un item", 400);
        }


        OrderTotals totales = orderPricing.calcular(
                orderResponse.getItems(),
                orderResponse.getDiscounts(),
                orderResponse.getAplicaItbms(),
                orderResponse.getDeliveryTypeId());

        Order order = new Order();
        order.setCustomer(customer);
        order.setPaymentMethod(orderResponse.getPaymentMethod());
        order.setDate_placed(LocalDateTime.now());
        order.setCreated_at(LocalDateTime.now());
        order.setOrder_number("TMP-" + UUID.randomUUID());
        order.setCreatedBy(createdByUserId != null ? userRepository.findById(createdByUserId).orElse(null) : null);


        boolean deInvitado = OrderCreatedEvent.ORIGIN_GUEST.equals(origin);

        order.setStatus(!deInvitado && PaymentMethods.estaCobrada(orderResponse.getPaymentMethod())
                ? STATUS_COMPLETED
                : STATUS_PENDING);

        order.setKitchenStatus(KITCHEN_PENDING);

        aplicarTotales(order, totales, orderResponse.getAplicaItbms());
        order.setComments(orderResponse.getComments());
        order.setIdempotencyKey(orderResponse.getIdempotencyKey());
        aplicarCobroEfectivo(order, orderResponse);

        for (OrderItem itemDto : orderResponse.getItems()) {
            order.addOrderItem(copiarLinea(itemDto));
        }

        try {
            orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException e) {
            if (orderResponse.getIdempotencyKey() != null) {
                return orderRepository.findByIdempotencyKey(orderResponse.getIdempotencyKey())
                        .orElseThrow(() -> new BusinessException("No se pudo registrar la orden", 409));
            }
            throw e;
        }

        order.setOrder_number(buildOrderNumber(order));
        orderRepository.saveAndFlush(order);

        guardarDescuentos(order.getOrderId(), totales);

        finalizarYAnunciar(order, customer, origin);

        return order;
    }

    /**
     * Cola común de {@code createOrder} y {@code confirmarCotizacion}: carga el
     * crédito si aplica y anuncia la orden a cocina/push. Una cotización en
     * DRAFT no pasa por aquí hasta que se confirma.
     */
    private void finalizarYAnunciar(Order order, Customer customer, String origin) {

        if (PaymentMethods.esCredito(order.getPaymentMethod())) {
            creditService.cargar(
                    customer.getCustomer_id(),
                    BigDecimal.valueOf(order.getTotal() != null ? order.getTotal() : 0d),
                    order.getOrderId(),
                    "Pedido " + order.getOrder_number(),
                    null);
        }

        events.publishEvent(

            new OrderCreatedEvent(
                order.getOrderId(),
                order.getOrder_number(),
                customerFullName(customer),
                order.getTotal(),
                origin,
                order.getDate_placed()
            ));

        if (STATUS_COMPLETED.equalsIgnoreCase(order.getStatus())) {
            events.publishEvent(new OrderCompletedEvent(order.getOrderId(), order.getOrder_number()));
        }
    }

    // -------------------------------------------------------------------
    // Ventas — listado unificado. La idea de una "cotización" con su propio
    // flujo de creación/edición se descartó (ver backup en _backups/); lo que
    // queda es solo lo que otras pantallas ya usan de verdad: el link/correo
    // de un documento existente (ver más abajo) y este listado.
    // -------------------------------------------------------------------

    /** Lista unificada de Ventas (módulo Ventas): cualquier orden, cotizaciones incluidas. */
    @Transactional(readOnly = true)
    public Page<com.service.lacasita.dto.SaleSummary> buscarVentas(String status, String search,
            Long paymentMethodId, String desdeStr, String hastaStr, int page, int size) {

        String st = (status == null || status.isBlank()) ? null : status.toUpperCase();
        String q = search == null ? "" : search.trim();

        LocalDateTime desde = (desdeStr == null || desdeStr.isBlank())
                ? null : LocalDate.parse(desdeStr).atStartOfDay();
        LocalDateTime hasta = (hastaStr == null || hastaStr.isBlank())
                ? null : LocalDate.parse(hastaStr).plusDays(1).atStartOfDay();

        return orderRepository.buscarVentas(st, q, paymentMethodId, desde, hasta, PageRequest.of(page, size));
    }

    /**
     * Genera (si hace falta) el link público de solo lectura de una orden —
     * cotización, prefactura o factura, según su estado (ver {@link #etiquetaDocumento}).
     */
    @Transactional
    public String generarLinkCotizacion(Long orderId) {

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException("Orden no encontrada: " + orderId, 404));

        if (order.getShareToken() == null || order.getShareToken().isBlank()) {
            order.setShareToken(UUID.randomUUID().toString().replace("-", ""));
            orderRepository.save(order);
        }

        return publicSiteBaseUrl + "/documento/" + order.getShareToken();
    }

    /**
     * El HTML que recibiría el cliente, sin enviarlo — para previsualizar antes de mandar.
     */
    @Transactional
    public String previsualizarCorreoCotizacion(Long orderId, Long plantillaId) {

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada: " + orderId, 404));

        EmailTemplate plantilla = resolverPlantilla(plantillaId);
        String link = generarLinkCotizacion(orderId);

        return EmailTemplateService.sustituir(plantilla.getHtml(), variablesCotizacion(order, link));
    }

    /**
     * Manda el link de la cotización por correo vía Resend.
     *
     * @param emailOverride destinatario a mano, para cuando el correo del cliente
     *                      registrado no es el correcto; en blanco usa ese correo.
     */
    @Transactional
    public void enviarCotizacionPorCorreo(Long orderId, Long plantillaId, String emailOverride) {

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada: " + orderId, 404));

        String destinatario = (emailOverride != null && !emailOverride.isBlank())
                ? emailOverride.trim()
                : (order.getCustomer() != null ? order.getCustomer().getEmail() : null);

        if (destinatario == null || destinatario.isBlank()) {
            throw new BusinessException("Indica un correo para enviar la cotización", 400);
        }

        EmailTemplate plantilla = resolverPlantilla(plantillaId);
        String link = generarLinkCotizacion(orderId);
        Map<String, String> valores = variablesCotizacion(order, link);

        String asunto = EmailTemplateService.sustituir(plantilla.getAsunto(), valores);
        String html = EmailTemplateService.sustituir(plantilla.getHtml(), valores);

        boolean enviado = resendEmailService.enviar(destinatario, asunto, html);
        if (!enviado) {
            throw new BusinessException("No se pudo enviar el correo. Intenta de nuevo en unos minutos.", 502);
        }
    }

    /** Con id explícito usa esa plantilla (debe estar activa); sin id, la primera activa. */
    private EmailTemplate resolverPlantilla(Long plantillaId) {

        if (plantillaId != null) {
            EmailTemplate plantilla = emailTemplateService.obtener(plantillaId);
            if (!Boolean.TRUE.equals(plantilla.getActiva())) {
                throw new BusinessException("La plantilla '%s' está desactivada".formatted(plantilla.getNombre()), 400);
            }
            return plantilla;
        }

        return emailTemplateService.listarActivas().stream().findFirst()
                .orElseThrow(() -> new BusinessException(
                        "No hay ninguna plantilla de correo activa. Crea una en Configuración → Plantillas.", 404));
    }

    private static final DateTimeFormatter FORMATO_FECHA_COTIZACION =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "PA"));

    private Map<String, String> variablesCotizacion(Order order, String link) {

        NumberFormat moneda = NumberFormat.getCurrencyInstance(new Locale("es", "PA"));

        Map<String, String> valores = new HashMap<>();
        valores.put("cliente", order.getCustomer() != null && order.getCustomer().getName() != null
                ? order.getCustomer().getName() : "");
        valores.put("numero_orden", order.getOrder_number());
        valores.put("total", moneda.format(order.getTotal() != null ? order.getTotal() : 0));
        valores.put("link", link);
        valores.put("restaurante", restaurantName);
        valores.put("fecha", order.getDate_placed() != null
                ? order.getDate_placed().format(FORMATO_FECHA_COTIZACION) : "");
        valores.put("logo", businessConfigService.getNegocio().logo());
        valores.put("imagen", businessConfigService.getNegocio().imagenCotizacion());
        valores.put("documento", etiquetaDocumento(order));
        return valores;
    }

    /**
     * "cotización" / "factura" / "prefactura" según el estado real de la orden —
     * la pantalla de Ventas abre tanto cotizaciones como órdenes de POS con el
     * mismo componente, así que el correo no puede asumir que siempre es una
     * cotización. Las tres palabras son femeninas a propósito, para que
     * "tu {{documento}}"/"la {{documento}}" concuerde sin importar cuál salga.
     */
    private String etiquetaDocumento(Order order) {
        if (STATUS_DRAFT.equalsIgnoreCase(order.getStatus())) return "cotización";
        if (STATUS_COMPLETED.equalsIgnoreCase(order.getStatus())) return "factura";
        return "prefactura";
    }

    /**
     * El estado que le corresponde a una orden según su cobro.
     */
    private String estadoSegunCobro(String estadoPedido, PaymentMethod metodo) {

        if (STATUS_CANCELLED.equalsIgnoreCase(estadoPedido)) {
            return STATUS_CANCELLED;
        }

        if (PaymentMethods.estaCobrada(metodo)) {
            return STATUS_COMPLETED;
        }
        return estadoPedido != null && !estadoPedido.isBlank() ? estadoPedido : STATUS_PENDING;
    }

    /**
     * Copia una línea del request a una entidad nueva.
     */
    // Visible para OrderItemCopyTest, que vigila que no se olvide ningún campo.
    OrderItem copiarLinea(OrderItem dto) {

        OrderItem linea = new OrderItem();

        linea.setName(dto.getName());
        linea.setDescription(dto.getDescription());
        linea.setImage(dto.getImage());
        linea.setProductId(dto.getProductId());
        linea.setQuantity(dto.getQuantity());
        linea.setPrice(dto.getPrice());
        linea.setDescuento(dto.getDescuento() != null ? dto.getDescuento() : 0d);

        return linea;
    }

    /**
     * Guarda lo entregado y el vuelto solo si el pago fue en efectivo.
     */
    private void aplicarCobroEfectivo(Order order, OrderResponse orderResponse) {
        boolean efectivo = PaymentMethods.esEfectivo(orderResponse.getPaymentMethod());
        order.setCashReceived(efectivo ? orderResponse.getCashReceived() : null);
        order.setChangeGiven(efectivo ? orderResponse.getChangeGiven() : null);
    }

    /**
     * Vuelca el desglose calculado sobre la orden.
     */
    private void aplicarTotales(Order order, OrderTotals totales, Boolean aplicaItbms) {
        order.setSubtotal(totales.subtotal().doubleValue());
        order.setDescuento(totales.descuento().doubleValue());
        order.setImpuesto(totales.impuesto().doubleValue());
        order.setEnvio(totales.envio().doubleValue());
        order.setTipoDelivery(totales.tipoDelivery());
        order.setTotal(totales.totalDouble());
        order.setAplicaItbms(aplicaItbms == null || aplicaItbms);
    }

    /**
     * Deja registrados los descuentos aplicados.
     */
    private void guardarDescuentos(Long orderId, OrderTotals totales) {

        orderDiscountRepository.deleteByOrderId(orderId);

        if (totales.descuentos().isEmpty()) return;

        totales.descuentos().forEach(d -> d.setOrderId(orderId));
        orderDiscountRepository.saveAll(totales.descuentos());
    }

    /**
     * Número de orden legible: {@code S000003} para la orden con
     */
    private String buildOrderNumber(Order order) {
        return "S%06d".formatted(order.getOrderId());
    }

    private String nombreDe(UserModel u) {
        if (u == null) return null;
        return u.getLastname() == null || u.getLastname().isBlank()
                ? u.getName() : u.getName() + " " + u.getLastname();
    }

    /*
     * Metodo para mapear una orden /encontrarorden
     */
    @Transactional(readOnly = true)
    public OrderResponse OrderResponseMap(Long id) {

        Order order = getOrdersById(id);
        Customer customer = order.getCustomer();

        if (customer == null) {
            return null;
        }

        OrderResponse response = new OrderResponse(
            order.getOrderId(),
            order.getOrder_number(),
            customer.getCustomer_id(),
            customer.getName(),
            customer.getLast_name(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getAddress(),
            order.getStatus(),
            order.getComments(),
            order.getTotal(),
            order.getDate_placed(),
            order.getUpdated_at(),
            order.getCreated_at(),
            order.getPaymentMethod(),
            order.getItems(),
            order.getIdempotencyKey());
            response.setSubtotal(order.getSubtotal());
            response.setDescuento(order.getDescuento());
            response.setImpuesto(order.getImpuesto());
            response.setAplicaItbms(order.getAplicaItbms());
            response.setCashReceived(order.getCashReceived());
            response.setChangeGiven(order.getChangeGiven());
            response.setCreatedByName(nombreDe(order.getCreatedBy()));
            response.setDiscounts(descuentosAplicados(id)
        );
            response.setEnvio(order.getEnvio());
            response.setDeliveryType(order.getTipoDelivery());
            response.setDeliveryTypeId(order.getTipoDelivery() != null ? order.getTipoDelivery().getId() : null);

        return response;
    }


    private List<DiscountRequest> descuentosAplicados(Long orderId) {

        return orderDiscountRepository.findByOrderId(orderId).stream()
                .map(d -> new DiscountRequest(
                        d.getTipoDescuentoId(),
                        d.getProductId(),
                        d.getMonto() != null ? d.getMonto().doubleValue() : null,
                        d.getDocumento(),
                        d.getMotivo()))
                .toList();
    }

    /*
     * Metodo para actualizar una orden
     */
    @Transactional
    @CacheEvict(value = { "salesChart", "salesChartDaily", "dashboardCharts", "dashboardKpis" }, allEntries = true)
    public Order updateOrder(OrderResponse orderResponse) {


        Order order = orderRepository.findByIdWithItems(orderResponse.getId())
                .orElseThrow(() -> new BusinessException(
                        "Orden no encontrada: " + orderResponse.getId(), 404));

        Customer customer = customerService.getCustomerById(orderResponse.getCustomer_id())
                .orElseThrow(() -> new BusinessException(
                        "Cliente no encontrado: " + orderResponse.getCustomer_id(), 404));

        String previousStatus = order.getStatus();

        order.setCustomer(customer);
        order.setPaymentMethod(orderResponse.getPaymentMethod());
        order.setComments(orderResponse.getComments());
        order.setStatus(estadoSegunCobro(orderResponse.getStatus(), orderResponse.getPaymentMethod()));
        aplicarCobroEfectivo(order, orderResponse);

        OrderTotals totales = null;

        if (orderResponse.getItems() != null) {
            totales = orderPricing.calcular(
                    orderResponse.getItems(),
                    orderResponse.getDiscounts(),
                    orderResponse.getAplicaItbms() != null
                            ? orderResponse.getAplicaItbms()
                            : order.getAplicaItbms(),
                    orderResponse.getDeliveryTypeId());

            aplicarTotales(order, totales, orderResponse.getAplicaItbms() != null
                    ? orderResponse.getAplicaItbms()
                    : order.getAplicaItbms());
        }


        if (orderResponse.getItems() != null) {
            order.getItems().clear();
            for (OrderItem itemDto : orderResponse.getItems()) {
                order.addOrderItem(copiarLinea(itemDto));
            }
        }

        Order saved = orderRepository.save(order);

        if (totales != null) {
            orderRepository.flush();  
            guardarDescuentos(saved.getOrderId(), totales);
        }


        boolean generaDeuda = PaymentMethods.esCredito(saved.getPaymentMethod())
                && !STATUS_CANCELLED.equalsIgnoreCase(saved.getStatus());

        creditService.sincronizarCargoDeOrden(
                saved.getOrderId(),
                customer.getCustomer_id(),
                BigDecimal.valueOf(saved.getTotal() != null ? saved.getTotal() : 0d),
                generaDeuda,
                "Pedido " + saved.getOrder_number(),
                null);

        if (STATUS_COMPLETED.equalsIgnoreCase(saved.getStatus())
                && !STATUS_COMPLETED.equalsIgnoreCase(previousStatus)) {
            events.publishEvent(new OrderCompletedEvent(saved.getOrderId(), saved.getOrder_number()));
        }


        if (STATUS_CANCELLED.equalsIgnoreCase(saved.getStatus())) {
            salesRecognitionService.anularVenta(saved.getOrderId());
        } else {
            salesRecognitionService.sincronizarConOrden(saved);
        }

        return saved;
    }


    @Transactional(readOnly = true)
    public double getNumOrders(LocalDateTime today) {
        return orderRepository.getCountOrders(inicioDe(today), inicioDe(today).plusDays(1));
    }

    /** Importe de los pedidos de ese día, sin los anulados. */
    @Transactional(readOnly = true)
    public Double getTotalOrdersToday(LocalDateTime date) {
        return orderRepository.getTotalOrdersToday(inicioDe(date), inicioDe(date).plusDays(1));
    }


    private LocalDateTime inicioDe(LocalDateTime fecha) {
        return fecha.toLocalDate().atStartOfDay();
    }


    @Transactional(readOnly = true)
    public List<PaymentMethod> getPaymentMethod() {
        return pMthRep.findAll();
    }

    /*
     * Metodo para buscar un metodo de pago por id
     */
    @Transactional(readOnly = true)
    public PaymentMethod findPaymentMethod(Long id) {
        return pMthRep.findById(id).orElse(null);
    }


    /*
        * Metodo para mapear una orden a KitchenResponse
     */
    private KitchenResponse toKitchenResponse(Order order) {
        Customer customer = order.getCustomer();
        return new KitchenResponse(
                order.getOrderId(),
                order.getOrder_number(),
                customer != null ? customer.getCustomer_id() : null,
                customerFullName(customer),
                order.getKitchenStatus(),
                order.getComments(),
                order.getTotal(),
                order.getDate_placed(),
                order.getCreated_at(),
                order.getPaymentMethod(),
                order.getItems());
    }

    private String customerFullName(Customer customer) {
        if (customer == null) {
            return null;
        }
        String last = customer.getLast_name();
        return last == null || last.isBlank() ? customer.getName(): customer.getName() + " " + last;
    }
}
