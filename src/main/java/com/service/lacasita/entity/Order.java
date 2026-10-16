package com.service.lacasita.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;
    
    private String order_number;

    private String status;

    @Column(name = "kitchen_status", nullable = false)
    private String kitchenStatus = "PENDING";

    @CreationTimestamp
    private LocalDateTime date_placed;

    private Double total;

    private Double subtotal;

    private Double descuento;

    private Double impuesto;

    @Column(name = "aplica_itbms")
    private Boolean aplicaItbms;

    private String comments;

    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id")
    private Customer customer;
    
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "payment_method", referencedColumnName = "idmetodo")
    private PaymentMethod paymentMethod;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    private LocalDateTime updated_at;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;


    @Column(name = "idempotency_key", updatable = false)
    private UUID idempotencyKey;


    @Column(name = "cash_received")
    private Double cashReceived;

    @Column(name = "change_given")
    private Double changeGiven;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private UserModel createdBy;

    @Column(name = "quote_stage")
    private String quoteStage;

    @Column(name = "share_token")
    private String shareToken;

    @ManyToOne
    @JoinColumn(name = "tipo_delivery_id", referencedColumnName = "id")
    private DeliveryType tipoDelivery;

    @Column(name = "costo_envio")
    private Double envio;


    public void addOrderItem(OrderItem item) {
        if (items == null) items = new ArrayList<>();
        items.add(item);
        item.setOrder(this);
    }
}
