package com.service.lacasita.service.sales;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.service.lacasita.event.OrderCompletedEvent;

import lombok.RequiredArgsConstructor;

/**
 * Registra la venta cuando una orden se completa.
 * @author Jason Nunez
 */
@Component
@RequiredArgsConstructor
public class SalesRecognitionListener {

    private static final Logger log = LoggerFactory.getLogger(SalesRecognitionListener.class);

    private final SalesRecognitionService salesRecognitionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCompleted(OrderCompletedEvent event) {
        try {
            if (salesRecognitionService.registrarVenta(event.orderId())) {
                log.info("Venta registrada para la orden {}", event.orderNumber());
            }
        } catch (Exception e) {
            log.error("No se pudo registrar la venta de la orden {}: {}",
                    event.orderNumber(), e.getMessage(), e);
        }
    }
}
