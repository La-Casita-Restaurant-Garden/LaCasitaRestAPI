package com.service.lacasita.repository;

import com.service.lacasita.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /** Permisos de un rol específico */
    List<Permission> findByRoleId(Long roleId);

    /** Permisos de un rol para un módulo específico */
    @Query("SELECT p FROM Permission p WHERE p.role.id = :roleId AND p.moduleKey = :moduleKey")
    Permission findByRoleIdAndModuleKey(@Param("roleId") Long roleId, @Param("moduleKey") String moduleKey);

    /** Todos los módulos accesibles (canRead=true) para un rol */
    @Query("SELECT p FROM Permission p WHERE p.role.id = :roleId AND p.canRead = true")
    List<Permission> findReadableByRoleId(@Param("roleId") Long roleId);
}
