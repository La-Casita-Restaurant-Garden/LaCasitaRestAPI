package com.service.lacasita.dto;

import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;
import com.service.lacasita.entity.DeliveryType;
import com.service.lacasita.entity.OrderItem;
import com.service.lacasita.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long                  id;
    private String                order_number;
    private Long                  customer_id;
    private String                name;
    private String                lastname;
    private String                email;
    private String                phone;
    private String                address;
    private String                status;
    private String                comments;
    private Double                total;
    private LocalDateTime         date_placed;
    private LocalDateTime         updated_at;
    private LocalDateTime         created_at;
    private PaymentMethod         paymentMethod;
    private List<OrderItem>       items;
    private UUID                  idempotencyKey;
    private Double                subtotal;
    private Double                descuento;
    private Double                impuesto;
    private Boolean               aplicaItbms;
    private List<DiscountRequest> discounts;
    private Double                cashReceived;
    private Double                changeGiven;
    private String                createdByName;

    /** Lo único que manda el cliente para elegir/cambiar el delivery; null = sin delivery. */
    private Long                  deliveryTypeId;
    /** Monto de envío ya calculado por el servidor; se ignora si el cliente lo manda. */
    private Double                envio;
    /** El tipo elegido, para mostrarlo sin una segunda consulta (igual que `paymentMethod`). */
    private DeliveryType          deliveryType;

    /**
     * Constructor historico, sin desglose.
     */
    public OrderResponse(Long id, String order_number, Long customer_id, String name, String lastname, String email, String phone, String address, String status, String comments, Double total,
            LocalDateTime date_placed, LocalDateTime updated_at, LocalDateTime created_at, PaymentMethod paymentMethod, List<OrderItem> items, UUID idempotencyKey) {

        this.id             = id;
        this.order_number   = order_number;
        this.customer_id    = customer_id;
        this.name           = name;
        this.lastname       = lastname;
        this.email          = email;
        this.phone          = phone;
        this.address        = address;
        this.status         = status;
        this.comments       = comments;
        this.total          = total;
        this.date_placed    = date_placed;
        this.updated_at     = updated_at;
        this.created_at     = created_at;
        this.paymentMethod  = paymentMethod;
        this.items          = items;
        this.idempotencyKey = idempotencyKey;
    }
}
