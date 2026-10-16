/* 
 * OrderCustomerResp.java
 * Jason D. Nunez
 * Copyright (c) 2026, Lacasita
 * DTO para obtener las ordenes de un cliente
 */
package com.service.lacasita.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class OrderCustomerResp {

    private Long id;
    private String order_number;
    private String name;
    private String status;
    private String paymentMethod;
    private Integer items;
    private LocalDateTime date_placed;
    private Double total;
    private String createdByName;
}
