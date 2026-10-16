package com.service.lacasita.service.config;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.dto.BusinessInfo;
import com.service.lacasita.dto.OrderDefaults;
import com.service.lacasita.entity.Configuracion;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.ConfiguracionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Parámetros del negocio.
 *
 * <p>Lecturas tolerantes a propósito: si falta la fila o el valor está mal escrito
 * se usa el valor por defecto y queda un aviso en el log. Un error de tecleo en
 * Configuraciones no puede impedir que se cobre un pedido.
 *
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class BusinessConfigService {

    private static final Logger log = LoggerFactory.getLogger(BusinessConfigService.class);

    /** Tasa vigente en Panamá para comida preparada. */
    public static final BigDecimal ITBMS_POR_DEFECTO = new BigDecimal("7.00");

    private final ConfiguracionRepository repository;

    @Transactional(readOnly = true)
    public BigDecimal getItbmsTasa() {

        String valor = leer(Configuracion.ITBMS_TASA);
        if (valor == null) return ITBMS_POR_DEFECTO;

        try {
            BigDecimal tasa = new BigDecimal(valor.trim());
            // Una tasa negativa o descomunal es un error de captura, no una política.
            if (tasa.signum() < 0 || tasa.compareTo(BigDecimal.valueOf(100)) > 0) {
                log.warn("Tasa de ITBMS fuera de rango ({}); se usa {}", valor, ITBMS_POR_DEFECTO);
                return ITBMS_POR_DEFECTO;
            }
            return tasa;
        } catch (NumberFormatException e) {
            log.warn("Tasa de ITBMS ilegible ('{}'); se usa {}", valor, ITBMS_POR_DEFECTO);
            return ITBMS_POR_DEFECTO;
        }
    }

    /** Interruptor general. Sin fila configurada el impuesto se cobra. */
    @Transactional(readOnly = true)
    public boolean isItbmsActivo() {
        String valor = leer(Configuracion.ITBMS_ACTIVO);
        return valor == null || Boolean.parseBoolean(valor.trim());
    }

    @Transactional(readOnly = true)
    public String leer(String clave) {
        return repository.findById(clave).map(Configuracion::getValor).orElse(null);
    }

    // ---------------------------------------------------------------------
    // Datos del negocio
    // ---------------------------------------------------------------------

    /**
     * Cabecera del comprobante y datos generales.
     *
     * <p>Una sola consulta para todas las claves: la tabla es diminuta y siete
     * lecturas por PK para pintar un ticket sería absurdo.
     */
    @Transactional(readOnly = true)
    public BusinessInfo getNegocio() {

        Map<String, String> valores = repository.findAll().stream()
                .collect(Collectors.toMap(Configuracion::getClave, Configuracion::getValor));

        return new BusinessInfo(
                valores.getOrDefault(Configuracion.NEGOCIO_NOMBRE, "La Casita"),
                valores.getOrDefault(Configuracion.NEGOCIO_SLOGAN, ""),
                valores.getOrDefault(Configuracion.NEGOCIO_TELEFONO, ""),
                valores.getOrDefault(Configuracion.NEGOCIO_EMAIL, ""),
                valores.getOrDefault(Configuracion.NEGOCIO_DIRECCION, ""),
                valores.getOrDefault(Configuracion.NEGOCIO_MONEDA, "USD"),
                valores.getOrDefault(Configuracion.NEGOCIO_ZONA, "America/Panama"),
                valores.getOrDefault(Configuracion.NEGOCIO_LOGO, ""),
                valores.getOrDefault(Configuracion.NEGOCIO_IMAGEN_COTIZACION, ""));
    }

    /**
     * Guarda los datos del negocio.
     *
     * <p>Un campo null se deja como está: permite actualizar solo lo que cambió sin
     * tener que reenviar todo el formulario.
     */
    @Transactional
    public BusinessInfo guardarNegocio(BusinessInfo info) {

        if (info.name() != null && info.name().isBlank()) {
            // El nombre encabeza cada comprobante; en blanco daría tickets anónimos.
            throw new BusinessException("El nombre del negocio no puede quedar vacío", 400);
        }

        guardarSiViene(Configuracion.NEGOCIO_NOMBRE, info.name());
        guardarSiViene(Configuracion.NEGOCIO_SLOGAN, info.slogan());
        guardarSiViene(Configuracion.NEGOCIO_TELEFONO, info.phone());
        guardarSiViene(Configuracion.NEGOCIO_EMAIL, info.email());
        guardarSiViene(Configuracion.NEGOCIO_DIRECCION, info.address());
        guardarSiViene(Configuracion.NEGOCIO_MONEDA, info.currency());
        guardarSiViene(Configuracion.NEGOCIO_ZONA, info.timezone());
        guardarSiViene(Configuracion.NEGOCIO_LOGO, info.logo());
        guardarSiViene(Configuracion.NEGOCIO_IMAGEN_COTIZACION, info.imagenCotizacion());

        return getNegocio();
    }

    private void guardarSiViene(String clave, String valor) {
        if (valor != null) guardar(clave, valor.trim(), null);
    }

    // ---------------------------------------------------------------------
    // Valores por defecto de crear orden
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderDefaults getOrderDefaults() {
        return new OrderDefaults(
                leerLong(Configuracion.ORDEN_CLIENTE_DEFECTO_ID),
                leerLong(Configuracion.ORDEN_CATEGORIA_DEFECTO_ID));
    }

    @Transactional
    public OrderDefaults guardarOrderDefaults(Long clienteId, Long categoriaId) {
        guardarLong(Configuracion.ORDEN_CLIENTE_DEFECTO_ID, clienteId);
        guardarLong(Configuracion.ORDEN_CATEGORIA_DEFECTO_ID, categoriaId);
        return getOrderDefaults();
    }

    private Long leerLong(String clave) {
        String valor = leer(clave);
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            log.warn("Valor ilegible en configuración '{}': '{}'", clave, valor);
            return null;
        }
    }

    /** {@code null} borra la clave: vuelve a "sin configurar", no a cero. */
    private void guardarLong(String clave, Long valor) {
        if (valor == null) {
            repository.deleteById(clave);
        } else {
            guardar(clave, valor.toString(), null);
        }
    }

    @Transactional
    public Configuracion guardar(String clave, String valor, String descripcion) {

        Configuracion c = repository.findById(clave).orElseGet(() -> {
            Configuracion nueva = new Configuracion();
            nueva.setClave(clave);
            return nueva;
        });

        c.setValor(valor);
        if (descripcion != null) c.setDescripcion(descripcion);

        return repository.save(c);
    }
}
