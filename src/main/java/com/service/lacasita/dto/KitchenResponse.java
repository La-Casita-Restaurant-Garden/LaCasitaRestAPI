package com.service.lacasita.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.service.lacasita.entity.OrderItem;
import com.service.lacasita.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KitchenResponse {
    
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String name;
    private String kitchenStatus;
    private String comments;
    private Double total;
    private LocalDateTime date_placed;
    private LocalDateTime created_at;
    private PaymentMethod paymentMethod;
    private List<OrderItem> items;
}
