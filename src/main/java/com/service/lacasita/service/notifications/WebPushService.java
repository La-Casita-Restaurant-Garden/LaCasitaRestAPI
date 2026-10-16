package com.service.lacasita.service.notifications;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.service.lacasita.entity.PushSubscription;
import com.service.lacasita.repository.PushSubscriptionRepository;

import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

/**
 * Empuja notificaciones del sistema operativo al navegador, aunque la
 * aplicación esté cerrada.
 * @author Jason Nunez
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    @Value("${webpush.vapid.public-key}")
    private String publicKey;

    @Value("${webpush.vapid.private-key}")
    private String privateKey;

    @Value("${webpush.vapid.subject}")
    private String subject;

    private final PushSubscriptionRepository subscriptionRepository;
    private final PushSubscriptionService subscriptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PushService pushService;

    public WebPushService(PushSubscriptionRepository subscriptionRepository,
                           PushSubscriptionService subscriptionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    @PostConstruct
    void init() {
        Security.addProvider(new BouncyCastleProvider());

        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("VAPID_PUBLIC_KEY/VAPID_PRIVATE_KEY sin configurar: "
                    + "las notificaciones push del navegador quedan desactivadas");
            return;
        }

        try {
            pushService = new PushService(publicKey, privateKey, subject);
        } catch (GeneralSecurityException e) {
            log.error("No se pudieron cargar las claves VAPID", e);
        }
    }

    /** La clave pública, para que el navegador pida la suscripción. Vacía si el servicio no está configurado. */
    public String publicKey() {
        return publicKey == null ? "" : publicKey;
    }

    /** Empuja a todos los dispositivos del usuario. Una suscripción caducada se borra sola. */
    public void enviarATodos(Long userId, String titulo, String mensaje, Long orderId) {

        if (pushService == null) return;

        List<PushSubscription> suscripciones = subscriptionRepository.findByUserId(userId);
        if (suscripciones.isEmpty()) return;

        String payload = payload(titulo, mensaje, orderId);
        suscripciones.forEach(sub -> enviar(sub, payload));
    }

    private void enviar(PushSubscription sub, String payload) {
        try {
            Subscription subscription = new Subscription(
                    sub.getEndpoint(), new Subscription.Keys(sub.getP256dh(), sub.getAuth()));

            var response = pushService.send(new Notification(subscription, payload));
            HttpStatusCode status = HttpStatusCode.valueOf(response.getStatusLine().getStatusCode());

            if (status == HttpStatus.NOT_FOUND || status == HttpStatus.GONE) {
                // El navegador dio de baja la suscripción (desinstaló la app, borró datos...).
                subscriptionService.desuscribir(sub.getEndpoint());
            } else if (status.isError()) {
                log.warn("Push rechazado ({}) para el dispositivo {}", status.value(), sub.getId());
            }
        } catch (Exception e) {
            // Un dispositivo que no responde no debe tumbar el envío a los demás.
            log.warn("No se pudo enviar la notificación push al dispositivo {}: {}", sub.getId(), e.getMessage());
        }
    }

    private String payload(String titulo, String mensaje, Long orderId) {
        try {
            ObjectNode raiz = objectMapper.createObjectNode();
            ObjectNode notificacion = raiz.putObject("notification");
            notificacion.put("title", titulo);
            notificacion.put("body", mensaje);
            notificacion.put("icon", "/assets/icon/favicon.png");
            notificacion.put("tag", "lacasita-notif");

            ObjectNode data = notificacion.putObject("data");
            if (orderId != null) data.put("orderId", orderId);

            return objectMapper.writeValueAsString(raiz);
        } catch (Exception e) {
            return "{}";
        }
    }
}
