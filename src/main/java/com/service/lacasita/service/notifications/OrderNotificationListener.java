package com.service.lacasita.service.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.service.lacasita.event.OrderCreatedEvent;

import lombok.RequiredArgsConstructor;

/**
 * Envia notificaciones de nueva orden.
 * @author Jason Nunez
 */
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            notificationService.notifyNewOrder(event);
        } catch (Exception e) {
            // No se puede notificar la orden.
            log.error("Fallo al notificar la orden {}", event.orderNumber(), e);
        }
    }
}
