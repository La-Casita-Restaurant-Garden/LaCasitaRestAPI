package com.service.lacasita.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

/**
 * Excepción de permisos para un usuario concreto.
 * @author Jason Nunez
 */
@Entity
@Table(name = "permisos_usuario")
@IdClass(UserPermissionOverride.UserPermissionOverrideId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionOverride {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "module_key", length = 50)
    private String moduleKey;

    @Column(name = "can_read")
    private Boolean canRead;

    @Column(name = "can_write")
    private Boolean canWrite;

    @Column(name = "can_delete")
    private Boolean canDelete;
    
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Motivo. Sin él, en seis meses nadie se atreve a retirar la excepción. */
    private String reason;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** ¿Sigue teniendo efecto? */
    public boolean isVigente() {
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPermissionOverrideId implements Serializable {
        private Long userId;
        private String moduleKey;
    }
}
