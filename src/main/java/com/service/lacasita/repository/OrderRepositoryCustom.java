package com.service.lacasita.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.service.lacasita.dto.SaleSummary;

/**
 * Búsqueda de Ventas con filtros opcionales. Fragmento aparte (en vez de un
 * {@code @Query} más) porque ninguna combinación de {@code :param IS NULL} le
 * gustó a Postgres: primero "could not determine data type" (el parámetro
 * solo aparecía en el IS NULL, sin contexto de tipo), después "cannot cast
 * bytea to bigint" al forzar el CAST (pgjdbc codifica un null sin tipo como
 * bytea en modo binario). La única forma robusta es no mandar el parámetro
 * cuando el filtro no viene — ver {@link OrderRepositoryCustomImpl}.
 */
public interface OrderRepositoryCustom {

    Page<SaleSummary> buscarVentas(String status, String search, Long paymentMethodId,
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
