package com.service.lacasita.service.notifications;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.service.lacasita.entity.Notification;
import com.service.lacasita.event.OrderCreatedEvent;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.UserRespository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final Set<String> ROLES_NUEVA_ORDEN = Set.of("ADMIN", "MANAGER", "WAITER", "KITCHEN");
    private static final Set<String> ROLES_STOCK = Set.of("ADMIN", "MANAGER");
    private static final Set<String> ROLES_ADMIN = Set.of("ADMIN");

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationStore store;
    private final UserRespository userRepository;
    private final WebPushService webPushService;

    public List<Notification> getMyNotification(long userId) {
        return store.listar(userId);
    }

    public Notification createNotification(Notification entrada) {

        Notification n = nueva(
                entrada.getIdUsuario(),
                entrada.getTitulo(),
                entrada.getMensaje(),
                entrada.getTipo() != null ? entrada.getTipo() : Notification.TIPO_GENERAL,
                entrada.getOrderNumber(),
                entrada.getOrderId());

        if (store.esPersistible(n.getTipo())) {
            store.guardar(n.getIdUsuario(), n);
        }
        sendToUser(n.getIdUsuario(), n);

        return n;
    }

    /** Busca dentro de la bandeja del propio usuario: no hay forma de pedir la de otro. */
    public Notification encontrarDeUsuario(Long userId, Long idNotificacion) {
        Notification n = store.buscar(userId, idNotificacion);
        if (n == null) {
            throw new BusinessException("Notificación no encontrada", 404);
        }
        return n;
    }

    public void deleteNotification(Long userId, Long idNotificacion) {
        store.eliminar(userId, idNotificacion);
    }

    public void markAsRead(Long userId, Long idNotificacion) {
        store.marcarLeida(userId, idNotificacion);
    }

    /** @return cuántas se marcaron */
    public int marcarTodasLeidas(Long userId) {
        return store.marcarTodasLeidas(userId);
    }

    /**
     * Empuja una notificación por WebSocket al destino privado del usuario.
     *
     * El destino es el id numérico porque así lo registra el interceptor STOMP
     * ({@code WebSocketConfig}) al autenticar el frame CONNECT.
     */
    public void sendToUser(Long userId, Notification notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification);
        } catch (Exception e) {
            // Un usuario desconectado no debe tumbar el envío a los demás: si el tipo
            // es persistible ya quedó en su bandeja y la verá al reconectar.
            log.warn("No se pudo entregar la notificación al usuario {}: {}", userId, e.getMessage());
        }

        // Aparte del WebSocket: llega aunque la pestaña esté cerrada. No depende de
        // si el tipo se guarda en la bandeja —eso es otra decisión— así que una
        // orden nueva sí puede despertar el teléfono aunque nunca quede en el
        // historial.
        webPushService.enviarATodos(
                userId, notification.getTitulo(), notification.getMensaje(), notification.getOrderId());
    }

  
    public void notifyNewOrder(OrderCreatedEvent event) {

        String origen = OrderCreatedEvent.ORIGIN_GUEST.equals(event.origin()) ? " (web)" : "";

        broadcast(
                ROLES_NUEVA_ORDEN,
                "Nueva orden " + event.orderNumber() + origen,
                "%s · %s".formatted(
                        event.customerName() != null ? event.customerName() : "Cliente",
                        formatTotal(event.total())),
                Notification.TIPO_NUEVA_ORDEN,
                event.orderNumber(),
                event.orderId());
    }

    /** Aviso a los administradores; hoy solo lo usan las solicitudes de acceso. */
    public void sendToUsersAdmin(Notification n) {
        broadcast(
                ROLES_ADMIN,
                n.getTitulo(),
                n.getMensaje(),
                n.getTipo() != null ? n.getTipo() : Notification.TIPO_GENERAL,
                n.getOrderNumber(),
                n.getOrderId());
    }

    /** Aviso de insumo por debajo del stock mínimo. */
    public void notifyLowStock(String itemName, String detalle) {
        broadcast(
                ROLES_STOCK,
                "Stock bajo: " + itemName,
                detalle,
                Notification.TIPO_STOCK_BAJO,
                null,
                null);
    }

    /**
     * Arma una notificación por destinatario, la guarda si el tipo lo amerita y
     * la empuja por WebSocket a todos, se haya guardado o no.
     */
    public void broadcast(Set<String> roles, String titulo, String mensaje,
            String tipo, String orderNumber, Long orderId) {

        List<Long> destinatarios = userRepository.findIdsByRoleNames(roles);

        if (destinatarios.isEmpty()) {
            log.warn("Notificación '{}' sin destinatarios: ningún usuario ACTIVE con roles {}", titulo, roles);
            return;
        }

        boolean persistible = store.esPersistible(tipo);

        for (Long userId : destinatarios) {
            Notification n = nueva(userId, titulo, mensaje, tipo, orderNumber, orderId);

            if (persistible) {
                store.guardar(userId, n);
            }
            sendToUser(userId, n);
        }
    }

    private Notification nueva(Long userId, String titulo, String mensaje, String tipo,
            String orderNumber, Long orderId) {

        LocalDateTime ahora = LocalDateTime.now();

        return Notification.builder()
                .idNotificacion(store.siguienteId())
                .idUsuario(userId)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipo != null ? tipo : Notification.TIPO_GENERAL)
                .orderNumber(orderNumber)
                .orderId(orderId)
                .leido(false)
                .fecha(ahora)
                .creado_en(ahora)
                .build();
    }

    private String formatTotal(Double total) {
        return total == null ? "" : "B/. %.2f".formatted(total);
    }
}
