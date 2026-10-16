package com.service.lacasita.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.UserModel;

@Repository
public interface UserRespository extends JpaRepository<UserModel, Long> {

    List<UserModel> findAllByOrderByName();

    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByUsername(String username);
    @Query("SELECT id FROM UserModel WHERE roleId.id = :rol")
    List<Long> findIdsByRol(Long rol);

    /**
     * Ids del personal activo cuyo rol está en la lista.
     *
     * Los nombres en {@code rolusuario} son ADMIN, WAITER, MANAGER y KITCHEN
     * (verificado contra la base el 2026-07-29). Se filtra por nombre y no por id
     * para no dejar el 1 incrustado en el código, y se excluye a los usuarios en
     * estado PENDING: son altas sin aprobar y no deben recibir avisos de operación.
     */
    @Query("""
            SELECT u.id FROM UserModel u
            WHERE upper(u.roleId.name) IN :roles
              AND (u.status IS NULL OR upper(u.status) = 'ACTIVE')
            """)
    List<Long> findIdsByRoleNames(@Param("roles") Collection<String> roles);
}
