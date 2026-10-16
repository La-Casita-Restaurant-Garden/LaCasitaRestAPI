package com.service.lacasita.entity;
import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private String name;

    @ManyToOne
    @JoinColumn(name = "category")
    private Category category;

    private String image;
    private String description;
    private Double price;
    private Date created_at;

    @Column(name = "aplica_itbms")
    private Boolean aplicaItbms;
    private Boolean activo;
    @Column(name = "show_in_menu")
    private Boolean showInMenu;

}