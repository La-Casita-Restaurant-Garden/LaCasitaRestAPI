package com.service.lacasita.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "conteo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conteo {

    public static final String ABIERTO = "ABIERTO";
    public static final String CERRADO = "CERRADO";
    public static final String ANULADO = "ANULADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha = LocalDate.now();

    @Column(nullable = false, length = 10)
    private String estado = ABIERTO;

    @Column(length = 255)
    private String nota;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "conteo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConteoLinea> lineas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "cerrado_at")
    private Instant cerradoAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
