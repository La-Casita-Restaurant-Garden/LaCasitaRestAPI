package com.service.lacasita.event;

import java.time.LocalDateTime;

/**
 * Se publica cuando una orden entra al sistema, venga del Manager, de la web
 * pública o de cualquier canal futuro.
 */
public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        String customerName,
        Double total,
        String origin,
        LocalDateTime placedAt) {

    public static final String ORIGIN_MANAGER = "MANAGER";
    public static final String ORIGIN_GUEST = "GUEST";
}
