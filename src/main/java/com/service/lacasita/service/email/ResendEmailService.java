package com.service.lacasita.service.email;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;

/**
 * Correo transaccional del negocio
 * @author Jason Nunez
 */
@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String remitentePorDefecto;

    private final RestClient http;

    public ResendEmailService(RestClient.Builder builder) {
        this.http = builder.baseUrl(API_URL).build();
    }

    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY sin configurar: el envío de correo queda desactivado");
        }
    }

    /**
     * @return true si Resend aceptó el envío.
     */
    public boolean enviar(String destinatario, String asunto, String html) {
        return enviar(remitentePorDefecto, destinatario, asunto, html);
    }

    /** Igual que {@link #enviar(String, String, String)}, con remitente propio. */
    public boolean enviar(String remitente, String destinatario, String asunto, String html) {

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Correo a '{}' no enviado: Resend no está configurado", destinatario);
            return false;
        }

        try {
            http.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", remitente,
                            "to", List.of(destinatario),
                            "subject", asunto,
                            "html", html))
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo a '{}': {}", destinatario, e.getMessage());
            return false;
        }
    }
}
