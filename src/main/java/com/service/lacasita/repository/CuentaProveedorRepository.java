package com.service.lacasita.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.CuentaProveedor;

/**
 * La deuda con proveedores.
 */
@Repository
public interface CuentaProveedorRepository extends JpaRepository<CuentaProveedor, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CuentaProveedor c
               SET c.saldo = c.saldo + :monto
             WHERE c.proveedorId = :proveedorId
            """)
    int cargar(@Param("proveedorId") Long proveedorId, @Param("monto") BigDecimal monto);

    /**
     * Baja el saldo solo si alcanza.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CuentaProveedor c
               SET c.saldo = c.saldo - :monto
             WHERE c.proveedorId = :proveedorId
               AND c.saldo >= :monto
            """)
    int abonar(@Param("proveedorId") Long proveedorId, @Param("monto") BigDecimal monto);

    @Query("SELECT c FROM CuentaProveedor c WHERE c.saldo > 0 ORDER BY c.saldo DESC")
    List<CuentaProveedor> findConDeuda();

    @Query("SELECT COALESCE(SUM(c.saldo), 0) FROM CuentaProveedor c WHERE c.saldo > 0")
    BigDecimal totalPorPagar();
}
