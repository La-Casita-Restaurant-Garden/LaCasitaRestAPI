package com.service.lacasita.entity;

import java.time.Instant;

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

/**
 * Plantilla de correo editable desde Settings. Los marcadores {@code {{clave}}}
 * en {@code asunto}/{@code html} se sustituyen al enviar — ver
 * {@link com.service.lacasita.service.email.EmailTemplateService#sustituir}.
 *
 * @author Jason Nunez
 */
@Entity
@Table(name = "plantilla_correo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, columnDefinition = "text")
    private String html;

    @Column(nullable = false)
    private Boolean activa;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
