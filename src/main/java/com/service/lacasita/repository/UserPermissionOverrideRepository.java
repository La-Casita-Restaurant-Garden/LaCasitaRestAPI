package com.service.lacasita.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.UserPermissionOverride;

@Repository
public interface UserPermissionOverrideRepository
        extends JpaRepository<UserPermissionOverride, UserPermissionOverride.UserPermissionOverrideId> {

    /**
     * Excepciones que TIENEN efecto ahora mismo.
     *
     * <p>El filtro de caducidad va aquí y no en Java para que una excepción vencida
     * no pueda colarse por olvidar comprobarla en alguna ruta de código.
     */
    @Query("""
            SELECT o FROM UserPermissionOverride o
            WHERE o.userId = :userId
              AND (o.expiresAt IS NULL OR o.expiresAt > :ahora)
            """)
    List<UserPermissionOverride> findVigentesByUser(@Param("userId") Long userId,
            @Param("ahora") Instant ahora);

    /** Todas las de un usuario, vigentes o no: para la pantalla de administración. */
    List<UserPermissionOverride> findByUserId(Long userId);

    /** Excepciones vigentes de todos los usuarios, para auditoría. */
    @Query("""
            SELECT o FROM UserPermissionOverride o
            WHERE o.expiresAt IS NULL OR o.expiresAt > :ahora
            ORDER BY o.userId, o.moduleKey
            """)
    List<UserPermissionOverride> findTodasVigentes(@Param("ahora") Instant ahora);

    @Modifying
    @Query("DELETE FROM UserPermissionOverride o WHERE o.userId = :userId AND o.moduleKey = :moduleKey")
    int deleteByUserAndModule(@Param("userId") Long userId, @Param("moduleKey") String moduleKey);
}
