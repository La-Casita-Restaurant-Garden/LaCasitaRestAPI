package com.service.lacasita.service.orders;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.service.lacasita.entity.Order;
import com.service.lacasita.entity.QuotePayment;
import com.service.lacasita.exception.BusinessException;
import com.service.lacasita.repository.OrderRepository;
import com.service.lacasita.repository.QuotePaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author Jason Nunez
 */
@Service
@RequiredArgsConstructor
public class QuotePaymentService {

    private static final String STATUS_DRAFT = "DRAFT";

    private final QuotePaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public QuotePayment registrar(Long orderId, BigDecimal monto, Long metodoPagoId,
            String nota, Long createdBy) {

        if (monto == null || monto.signum() <= 0) {
            throw new BusinessException("El monto debe ser mayor que cero", 400);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Cotización no encontrada: " + orderId, 404));

        if (!STATUS_DRAFT.equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException("Esta orden ya no está en borrador", 409);
        }

        return paymentRepository.save(QuotePayment.builder()
                .orderId(orderId)
                .monto(monto)
                .metodoPagoId(metodoPagoId)
                .nota(nota)
                .createdBy(createdBy)
                .build());
    }

    @Transactional(readOnly = true)
    public List<QuotePayment> historial(Long orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }
}
