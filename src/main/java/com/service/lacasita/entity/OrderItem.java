package com.service.lacasita.entity;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue
    @Column(name = "item_id", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;
    @Column(name = "product_id")
    private UUID productId;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private Double descuento;
    private Date delivered;
    private String image;

}
