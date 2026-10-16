package com.service.lacasita.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.OrderDiscount;

@Repository
public interface OrderDiscountRepository extends JpaRepository<OrderDiscount, Long> {

    List<OrderDiscount> findByOrderId(Long orderId);

    /**
     * Al reeditar un pedido se rehace el desglose entero.
     *
     * <p>Tiene que ser un DELETE de verdad (bulk, vía {@code @Modifying}), no el
     * {@code deleteByOrderId} derivado de siempre: ese hace un SELECT + remove()
     * por fila, y Hibernate ordena su flush DESPUÉS de las inserciones nuevas —
     * el guardado que sigue (insertar los descuentos recién calculados) llegaba
     * a la base antes de que este borrado se ejecutara, y chocaba con la fila
     * vieja contra el índice único {@code orden_descuento_orden_tipo_uidx}.
     * {@code flushAutomatically} fuerza que este DELETE salga ya, en su lugar.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM OrderDiscount d WHERE d.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * Descuentos concedidos en un periodo, por tipo.
     *
     * <p>Es la línea del cierre que explica por qué se cobró menos que el precio de
     * carta. Sin ella, un descuento y un faltante de caja son indistinguibles.
     *
     * <p>Devuelve {@code [tipoDescuentoId, cantidad, total]}.
     */
    @Query("""
            SELECT d.tipoDescuentoId, COUNT(d), COALESCE(SUM(d.monto), 0) FROM OrderDiscount d
             WHERE d.createdAt >= :desde AND d.createdAt <= :hasta
             GROUP BY d.tipoDescuentoId
            """)
    List<Object[]> resumenPorTipo(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
