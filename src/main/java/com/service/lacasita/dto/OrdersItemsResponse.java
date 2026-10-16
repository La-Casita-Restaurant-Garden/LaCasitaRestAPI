package com.service.lacasita.dto;

import java.util.List;

import com.service.lacasita.entity.OrderItem;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrdersItemsResponse {
    
    private OrderResponse order;
    private List<OrderItem> item;
    
}
