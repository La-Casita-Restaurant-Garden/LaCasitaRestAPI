package com.service.lacasita.service.email;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.service.lacasita.dto.EmailTemplateVariable;
import com.service.lacasita.entity.EmailTemplate;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.EmailTemplateRepository;

/**
 * Plantillas de correo con marcadores {@code {{clave}}}, editables desde
 * Settings sin tocar el backend.
 *
 * @author Jason Nunez
 */
@Service
public class EmailTemplateService {

    @Autowired
    private EmailTemplateRepository repository;

    public List<EmailTemplate> listarActivas() {
        return repository.findByActivaTrueOrderByNombreAsc();
    }

    public List<EmailTemplate> listarTodas() {
        return repository.findAll(Sort.by("nombre"));
    }

    public EmailTemplate obtener(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new BusinessException("Plantilla no encontrada: " + id, 404));
    }

    public EmailTemplate guardar(EmailTemplate body) {

        if (body.getNombre() == null || body.getNombre().isBlank()) {
            throw new BusinessException("La plantilla necesita un nombre", 400);
        }
        if (body.getAsunto() == null || body.getAsunto().isBlank()) {
            throw new BusinessException("La plantilla necesita un asunto", 400);
        }
        if (body.getHtml() == null || body.getHtml().isBlank()) {
            throw new BusinessException("La plantilla necesita contenido HTML", 400);
        }

        EmailTemplate plantilla = body.getId() == null
                ? EmailTemplate.builder().build()
                : obtener(body.getId());

        plantilla.setNombre(body.getNombre().trim());
        plantilla.setAsunto(body.getAsunto().trim());
        plantilla.setHtml(body.getHtml());
        plantilla.setActiva(body.getActiva() == null || body.getActiva());

        return repository.save(plantilla);
    }

    public EmailTemplate alternar(Long id) {
        EmailTemplate plantilla = obtener(id);
        plantilla.setActiva(!Boolean.TRUE.equals(plantilla.getActiva()));
        return repository.save(plantilla);
    }

    /** Sustituye cada {@code {{clave}}} por su valor; una clave sin valor queda vacía. */
    public static String sustituir(String texto, Map<String, String> valores) {
        if (texto == null) return "";

        String resultado = texto;
        for (Map.Entry<String, String> valor : valores.entrySet()) {
            resultado = resultado.replace("{{" + valor.getKey() + "}}", valor.getValue() == null ? "" : valor.getValue());
        }
        return resultado;
    }

    /**
     * Parámetros disponibles al enviar una cotización — documentados aquí, no
     * duplicados en el frontend, para que el editor de Settings los liste solos.
     */
    public static List<EmailTemplateVariable> variablesCotizacion() {
        return List.of(
                new EmailTemplateVariable("cliente", "Nombre del cliente", "Juan Pérez"),
                new EmailTemplateVariable("numero_orden", "Número de la cotización", "COT-000123"),
                new EmailTemplateVariable("total", "Total formateado en dólares", "$45.00"),
                new EmailTemplateVariable("link", "Link público de solo lectura del documento",
                        "https://lacasitarest.com/documento/abc123"),
                new EmailTemplateVariable("restaurante", "Nombre del negocio", "La Casita Restaurant & Garden"),
                new EmailTemplateVariable("fecha", "Fecha en que se creó la cotización", "6 de septiembre de 2026"),
                new EmailTemplateVariable("logo", "Logo del negocio, configurado en Settings → General",
                        "https://cdn.lacasitarest.com/logo.png"),
                new EmailTemplateVariable("imagen", "Imagen fija para el correo de cotizaciones, configurada en Settings → General",
                        "https://cdn.lacasitarest.com/cotizaciones-banner.jpg"),
                new EmailTemplateVariable("documento",
                        "\"cotización\", \"factura\" o \"prefactura\" según el estado real de la orden — "
                                + "úsalo en vez de escribir \"cotización\" a mano, porque este correo también se "
                                + "manda desde órdenes de POS que nunca fueron una cotización",
                        "factura"));
    }

    /** Valores de ejemplo para las claves de {@link #variablesCotizacion()}, listos para un preview. */
    public static Map<String, String> valoresDeEjemploCotizacion() {
        Map<String, String> valores = new HashMap<>();
        variablesCotizacion().forEach(v -> valores.put(v.clave(), v.ejemplo()));
        return valores;
    }
}
