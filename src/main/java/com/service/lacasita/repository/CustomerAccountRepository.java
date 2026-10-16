package com.service.lacasita.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.CustomerAccount;

@Repository
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {

   
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CustomerAccount a
               SET a.saldo = a.saldo + :monto
             WHERE a.customerId = :customerId
               AND a.activo = true
               AND a.saldo + :monto <= a.limiteCredito
            """)
    int cargar(@Param("customerId") Long customerId, @Param("monto") BigDecimal monto);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CustomerAccount a
               SET a.saldo = a.saldo - :monto
             WHERE a.customerId = :customerId
               AND a.saldo >= :monto
            """)
    int abonar(@Param("customerId") Long customerId, @Param("monto") BigDecimal monto);

    /**
     * Ajuste manual sin validar límite: es la vía para corregir y condonar.
     *
     * <p>No comprueba el techo a propósito —un ajuste puede dejar la cuenta por
     * encima del límite si el administrador así lo decide— pero sí impide que el
     * saldo quede negativo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CustomerAccount a
               SET a.saldo = a.saldo + :monto
             WHERE a.customerId = :customerId
               AND a.saldo + :monto >= 0
            """)
    int ajustar(@Param("customerId") Long customerId, @Param("monto") BigDecimal monto);

    /** Quién debe, de mayor a menor. Alimenta la pantalla de deudores. */
    @Query("SELECT a FROM CustomerAccount a WHERE a.saldo > 0 ORDER BY a.saldo DESC")
    List<CustomerAccount> findDeudores();

    @Query("SELECT COALESCE(SUM(a.saldo), 0) FROM CustomerAccount a WHERE a.saldo > 0")
    BigDecimal totalPorCobrar();
}
