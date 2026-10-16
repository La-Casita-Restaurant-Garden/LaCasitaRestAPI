package com.service.lacasita.service.orders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.service.lacasita.dto.DiscountRequest;
import com.service.lacasita.entity.DeliveryType;
import com.service.lacasita.entity.DiscountType;
import com.service.lacasita.entity.OrderDiscount;
import com.service.lacasita.entity.OrderItem;
import com.service.lacasita.entity.Product;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.DeliveryTypeRepository;
import com.service.lacasita.repository.DiscountTypeRepository;
import com.service.lacasita.repository.ProductRepository;
import com.service.lacasita.service.config.BusinessConfigService;
import com.service.lacasita.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderPricing {

    private static final Logger log = LoggerFactory.getLogger(OrderPricing.class);
    private static final int MAX_CANTIDAD = 999;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int ESCALA = 2;

    private final ProductRepository productRepository;
    private final DiscountTypeRepository discountTypeRepository;
    private final DeliveryTypeRepository deliveryTypeRepository;
    private final BusinessConfigService config;


    public OrderTotals calcular(List<OrderItem> items, List<DiscountRequest> descuentos,
            Boolean aplicaItbms, Long deliveryTypeId) {

        if (items == null || items.isEmpty()) {
            throw new BusinessException("La orden debe tener al menos un item", 400);
        }

        Map<UUID, Product> catalogo = cargarCatalogo(items);
        Map<OrderItem, BigDecimal> brutoPorLinea = new IdentityHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItem item : items) {
            int cantidad = exigirCantidadValida(item);
            BigDecimal precio = resolverPrecio(item, catalogo);

            item.setPrice(precio.doubleValue());
            item.setDescuento(0d);

            BigDecimal bruto = precio.multiply(BigDecimal.valueOf(cantidad));
            brutoPorLinea.put(item, bruto);
            subtotal = subtotal.add(bruto);
        }

        // --- 2. Descuentos --------------------------------------------------
        List<OrderDiscount> aplicados = new ArrayList<>();
        BigDecimal descuentoLinea = aplicarDescuentosDeLinea(descuentos, items, brutoPorLinea, aplicados);

        // Los descuentos de cuenta van sobre lo que queda tras los de línea: un 25 %
        // de jubilado sobre un plato ya rebajado se calcula del precio rebajado.
        BigDecimal netoAntesDeCuenta = subtotal.subtract(descuentoLinea);
        BigDecimal descuentoCuenta = aplicarDescuentosDeCuenta(descuentos, netoAntesDeCuenta, aplicados);

        BigDecimal descuento = descuentoLinea.add(descuentoCuenta);

        // --- 3. Base gravable e impuesto ------------------------------------
        BigDecimal gravableAntesDeCuenta = brutoPorLinea.entrySet().stream()
                .filter(e -> tributa(e.getKey(), catalogo))
                .map(e -> e.getValue().subtract(descuentoDe(e.getKey())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal baseGravable = repartirDescuentoDeCuenta(
                gravableAntesDeCuenta, netoAntesDeCuenta, descuentoCuenta);

        BigDecimal impuesto = calcularImpuesto(baseGravable, aplicaItbms);

        DeliveryType tipoDelivery = resolverTipoDelivery(deliveryTypeId);
        BigDecimal envio = tipoDelivery != null ? tipoDelivery.getMonto() : BigDecimal.ZERO;

        // --- 5. Total -------------------------------------------------------
        BigDecimal total = subtotal.subtract(descuento).add(impuesto).add(envio);

        return new OrderTotals(
                redondear(subtotal),
                redondear(descuento),
                redondear(baseGravable),
                redondear(impuesto),
                redondear(envio),
                redondear(total),
                aplicados,
                tipoDelivery);
    }

    /** Null si no se pidió delivery; si se pidió, exige que exista y esté activo. */
    private DeliveryType resolverTipoDelivery(Long deliveryTypeId) {

        if (deliveryTypeId == null) return null;

        DeliveryType tipo = deliveryTypeRepository.findById(deliveryTypeId).orElseThrow(() ->
                new BusinessException("Tipo de delivery no encontrado: " + deliveryTypeId, 404));

        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            throw new BusinessException(
                    "El tipo de delivery '%s' está desactivado".formatted(tipo.getNombre()), 400);
        }

        return tipo;
    }

    // =====================================================================
    // Precios
    // =====================================================================

    /** Una sola consulta para todo el pedido; buscar producto a producto sería N+1. */
    private Map<UUID, Product> cargarCatalogo(List<OrderItem> items) {

        List<UUID> ids = items.stream()
                .map(OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) return Map.of();

        return productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
    }

    private BigDecimal resolverPrecio(OrderItem item, Map<UUID, Product> catalogo) {

        if (item.getProductId() == null) {
            log.warn("Línea sin product_id ('{}'): se conserva el precio recibido {}",
                    item.getName(), item.getPrice());
            return exigirPrecioValido(item.getPrice(), item.getName());
        }

        Product producto = catalogo.get(item.getProductId());

        if (producto == null) {
            throw new BusinessException(
                    "El producto '%s' ya no existe en el catálogo".formatted(item.getName()), 404);
        }

        if (producto.getPrice() == null) {
            log.warn("Producto {} ('{}') sin precio en el catálogo; se usa el recibido {}",
                    producto.getId(), producto.getName(), item.getPrice());
            return exigirPrecioValido(item.getPrice(), item.getName());
        }

        BigDecimal delCatalogo = BigDecimal.valueOf(producto.getPrice());

        if (item.getPrice() != null
                && BigDecimal.valueOf(item.getPrice()).compareTo(delCatalogo) != 0) {
            log.info("Precio corregido en '{}': recibido {}, catálogo {}",
                    producto.getName(), item.getPrice(), delCatalogo);
        }

        return delCatalogo;
    }

    // =====================================================================
    // Descuentos
    // =====================================================================

    private BigDecimal aplicarDescuentosDeLinea(List<DiscountRequest> peticiones, List<OrderItem> items,
            Map<OrderItem, BigDecimal> brutoPorLinea, List<OrderDiscount> aplicados) {

        if (peticiones == null || peticiones.isEmpty()) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;

        for (DiscountRequest p : peticiones) {
            if (p == null || p.getProductId() == null) continue;

            DiscountType tipo = exigirTipo(p);

            if (tipo.esDeOrden()) {
                throw new BusinessException(
                        "'%s' es un descuento de cuenta y no puede aplicarse a un producto"
                                .formatted(tipo.getNombre()), 400);
            }

            // Todas las líneas del mismo producto: si alguien pidió dos veces lo
            // mismo, la promoción vale para las dos.
            List<OrderItem> objetivo = items.stream()
                    .filter(i -> p.getProductId().equals(i.getProductId()))
                    .toList();

            if (objetivo.isEmpty()) {
                throw new BusinessException(
                        "El descuento '%s' apunta a un producto que no está en el pedido"
                                .formatted(tipo.getNombre()), 400);
            }

            for (OrderItem item : objetivo) {
                BigDecimal bruto = brutoPorLinea.get(item);
                BigDecimal monto = calcularMonto(tipo, p, bruto);

                item.setDescuento(descuentoDe(item).add(monto).doubleValue());
                total = total.add(monto);

                // El producto y no la línea: al editar el pedido las líneas se
                // recrean con identificadores nuevos y la referencia quedaría rota.
                aplicados.add(construir(tipo, p, monto, item.getProductId()));
            }
        }

        return total;
    }

    private BigDecimal aplicarDescuentosDeCuenta(List<DiscountRequest> peticiones,
            BigDecimal base, List<OrderDiscount> aplicados) {

        if (peticiones == null || peticiones.isEmpty()) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;

        for (DiscountRequest p : peticiones) {
            if (p == null || p.getProductId() != null) continue;

            DiscountType tipo = exigirTipo(p);

            if (!tipo.esDeOrden()) {
                throw new BusinessException(
                        "'%s' es un descuento de producto: hay que indicar sobre cuál se aplica"
                                .formatted(tipo.getNombre()), 400);
            }

            BigDecimal monto = calcularMonto(tipo, p, base);

            total = total.add(monto);
            aplicados.add(construir(tipo, p, monto, null));
        }

        if (total.compareTo(base) > 0) {
            throw new BusinessException(
                    "Los descuentos (%s) superan el importe del pedido (%s)"
                            .formatted(redondear(total), redondear(base)), 400);
        }

        return total;
    }

    /**
     * Cuánto rebaja este tipo sobre una base.
     */
    private BigDecimal calcularMonto(DiscountType tipo, DiscountRequest peticion, BigDecimal base) {

        if (base.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal monto;

        if (tipo.getPorcentaje() != null) {
            monto = base.multiply(tipo.getPorcentaje())
                    .divide(CIEN, ESCALA, RoundingMode.HALF_UP);

        } else if (tipo.getMontoFijo() != null) {
            monto = tipo.getMontoFijo();

        } else {
            if (peticion.getMonto() == null || peticion.getMonto() <= 0) {
                throw new BusinessException(
                        "'%s' necesita un importe".formatted(tipo.getNombre()), 400);
            }
            monto = BigDecimal.valueOf(peticion.getMonto());
        }

        // Nunca más que la base: un monto fijo mayor que el pedido dejaría un total
        // negativo, y una cortesía del doble de la cuenta es un error de tecleo.
        return monto.min(base).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private DiscountType exigirTipo(DiscountRequest p) {

        if (p.getTipoId() == null) {
            throw new BusinessException("Falta indicar el tipo de descuento", 400);
        }

        DiscountType tipo = discountTypeRepository.findById(p.getTipoId()).orElseThrow(() ->
                new BusinessException("Tipo de descuento no encontrado: " + p.getTipoId(), 404));

        if (!Boolean.TRUE.equals(tipo.getActivo())) {
            throw new BusinessException(
                    "El descuento '%s' está desactivado".formatted(tipo.getNombre()), 400);
        }

        // Las rebajas discrecionales no están al alcance de cualquiera. El backend
        // decide: ocultar el botón en la pantalla no es un control.
        if (Boolean.TRUE.equals(tipo.getRequiereAdmin()) && !SecurityUtils.esAdmin()) {
            throw new BusinessException(
                    "Solo un administrador puede aplicar '%s'".formatted(tipo.getNombre()), 403);
        }

        if (Boolean.TRUE.equals(tipo.getRequiereMotivo())
                && (p.getMotivo() == null || p.getMotivo().isBlank())) {
            throw new BusinessException(
                    "El descuento '%s' exige un motivo".formatted(tipo.getNombre()), 400);
        }

        // Sin la cédula, el descuento de jubilado no se puede justificar después, y
        // reconstruirlo es imposible.
        if (Boolean.TRUE.equals(tipo.getRequiereDocumento())
                && (p.getDocumento() == null || p.getDocumento().isBlank())) {
            throw new BusinessException(
                    "El descuento '%s' exige la cédula del beneficiario".formatted(tipo.getNombre()), 400);
        }

        return tipo;
    }

    private OrderDiscount construir(DiscountType tipo, DiscountRequest p, BigDecimal monto, UUID productId) {
        return OrderDiscount.builder()
                .tipoDescuentoId(tipo.getId())
                .productId(productId)
                // El porcentaje vigente HOY, congelado: si mañana cambia, este pedido
                // conserva el que se le aplicó.
                .porcentaje(tipo.getPorcentaje())
                .monto(monto)
                .documento(p.getDocumento())
                .motivo(p.getMotivo())
                .build();
    }

    // =====================================================================
    // Impuesto
    // =====================================================================

    /**
     * Qué parte del neto corresponde a productos gravados.
     */
    private BigDecimal repartirDescuentoDeCuenta(BigDecimal gravableAntesDeCuenta,
            BigDecimal netoAntesDeCuenta, BigDecimal descuentoCuenta) {

        if (gravableAntesDeCuenta.signum() <= 0) return BigDecimal.ZERO;
        if (descuentoCuenta.signum() <= 0) return gravableAntesDeCuenta;
        if (netoAntesDeCuenta.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal proporcionRestante = netoAntesDeCuenta.subtract(descuentoCuenta)
                .divide(netoAntesDeCuenta, 10, RoundingMode.HALF_UP);

        return gravableAntesDeCuenta.multiply(proporcionRestante);
    }

    private BigDecimal calcularImpuesto(BigDecimal baseGravable, Boolean aplicaItbmsPedido) {

        // Null se trata como true: un pedido que no dice nada tributa con normalidad.
        if (Boolean.FALSE.equals(aplicaItbmsPedido)) return BigDecimal.ZERO;

        // El interruptor general manda sobre el del pedido.
        if (!config.isItbmsActivo()) return BigDecimal.ZERO;

        if (baseGravable.signum() <= 0) return BigDecimal.ZERO;

        return baseGravable.multiply(config.getItbmsTasa())
                .divide(CIEN, ESCALA, RoundingMode.HALF_UP);
    }

    /** Sin producto enlazado se asume gravado: es la norma en un restaurante. */
    private boolean tributa(OrderItem item, Map<UUID, Product> catalogo) {

        if (item.getProductId() == null) return true;

        Product p = catalogo.get(item.getProductId());
        return p == null || !Boolean.FALSE.equals(p.getAplicaItbms());
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private BigDecimal descuentoDe(OrderItem item) {
        return item.getDescuento() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getDescuento());
    }

    /**
     * HALF_UP y no el redondeo por defecto de double: estos importes van a una
     * cuenta por cobrar y a un ticket, y tienen que dar lo mismo aquí que en la
     * calculadora del dueño.
     */
    private BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private int exigirCantidadValida(OrderItem item) {

        Integer cantidad = item.getQuantity();

        if (cantidad == null || cantidad <= 0) {
            throw new BusinessException(
                    "Cantidad inválida en '%s': debe ser mayor que cero".formatted(item.getName()), 400);
        }

        if (cantidad > MAX_CANTIDAD) {
            throw new BusinessException(
                    "Cantidad excesiva en '%s': el máximo por línea es %d"
                            .formatted(item.getName(), MAX_CANTIDAD), 400);
        }

        return cantidad;
    }

    private BigDecimal exigirPrecioValido(Double precio, String nombre) {

        if (precio == null || precio < 0) {
            throw new BusinessException("Precio inválido en '%s'".formatted(nombre), 400);
        }

        return BigDecimal.valueOf(precio);
    }
}
