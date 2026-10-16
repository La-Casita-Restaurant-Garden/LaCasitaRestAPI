package com.service.lacasita.entity;

import java.sql.Date;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ct_id")
    private Long id;
    @Column(name = "ct_name")
    private String name;

    @Column(name = "ct_type")
    private String type;

    /** Si, siendo de menú, se ofrece hoy en el selector de productos al crear una orden. */
    @Column(name = "show_in_menu")
    private Boolean mostrarEnPedido;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonIgnore
    private Date created_at;

}