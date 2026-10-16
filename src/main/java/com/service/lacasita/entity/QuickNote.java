package com.service.lacasita.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;


@Entity
@Table(name = "nota_rapida")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickNote {

    /** Tope de longitud: una nota más larga no cabe en el chip de la interfaz. */
    public static final int MAX_TEXTO = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_TEXTO)
    private String texto;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Boolean activo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
