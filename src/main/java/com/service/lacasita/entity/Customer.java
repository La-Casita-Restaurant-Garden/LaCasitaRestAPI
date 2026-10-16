package com.service.lacasita.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customer_id;
    private String name;
    @Column(name = "last_name")
    private String last_name;
    private String email;
    private String phone;
    @ManyToOne
    @JoinColumn(name = "role", referencedColumnName = "rl_id")
    private Role role;
    private String address;
    @JsonIgnore
    private String password;
    private String status;
    
    public Customer(String name, String lastname, String email, String phone, String password) {
        this.name      = name;
        this.last_name = lastname;
        this.email     = email;
        this.phone     = phone;
        this.password  = password;
        this.role      = new Role();
        this.status    = "PENDING";
    }
}
