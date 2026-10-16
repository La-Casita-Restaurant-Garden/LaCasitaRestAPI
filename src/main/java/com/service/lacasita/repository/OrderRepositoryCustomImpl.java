package com.service.lacasita.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.service.lacasita.dto.SaleSummary;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Cada filtro de la lista de Ventas entra al WHERE solo si viene con valor —
 * nunca se pega un parámetro null. Ver el porqué en {@link OrderRepositoryCustom}.
 */
@Repository
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<SaleSummary> buscarVentas(String status, String search, Long paymentMethodId,
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {

        List<String> condiciones = new ArrayList<>();
        Map<String, Object> parametros = new LinkedHashMap<>();

        if (status != null && !status.isBlank()) {
            condiciones.add("o.status = :status");
            parametros.put("status", status);
        }
        if (paymentMethodId != null) {
            condiciones.add("m.idmetodo = :paymentMethodId");
            parametros.put("paymentMethodId", paymentMethodId);
        }
        if (desde != null) {
            condiciones.add("o.date_placed >= :desde");
            parametros.put("desde", desde);
        }
        if (hasta != null) {
            condiciones.add("o.date_placed < :hasta");
            parametros.put("hasta", hasta);
        }
        if (search != null && !search.isBlank()) {
            condiciones.add("""
                    (LOWER(o.order_number) LIKE :search
                     OR LOWER(c.name) LIKE :search
                     OR LOWER(c.last_name) LIKE :search)
                    """);
            parametros.put("search", "%" + search.trim().toLowerCase() + "%");
        }

        String where = condiciones.isEmpty() ? "" : "WHERE " + String.join(" AND ", condiciones);

        String jpql = """
                SELECT new com.service.lacasita.dto.SaleSummary(
                         o.orderId, o.order_number, o.status, o.quoteStage, o.total, o.date_placed,
                         c.name, c.last_name, m.name, u.name, u.lastname)
                  FROM Order o
                  LEFT JOIN o.customer c
                  LEFT JOIN o.paymentMethod m
                  LEFT JOIN o.createdBy u
                """ + where + " ORDER BY o.date_placed DESC";

        String countJpql = """
                SELECT COUNT(o) FROM Order o
                  LEFT JOIN o.customer c
                  LEFT JOIN o.paymentMethod m
                """ + where;

        TypedQuery<SaleSummary> query = em.createQuery(jpql, SaleSummary.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);

        parametros.forEach((nombre, valor) -> {
            query.setParameter(nombre, valor);
            countQuery.setParameter(nombre, valor);
        });

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<SaleSummary> contenido = query.getResultList();
        long total = countQuery.getSingleResult();

        return new PageImpl<>(contenido, pageable, total);
    }
}
