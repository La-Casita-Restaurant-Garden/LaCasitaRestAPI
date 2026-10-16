package com.service.lacasita.entity;

// Ver PaymentMethods (clase de utilidad) para reconocer efectivo y crédito por
// nombre: la tabla metodopago no tiene una columna de tipo.

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "metodopago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idmetodo;
    private String name;
    @JsonIgnore
    private Date created_at;
}
