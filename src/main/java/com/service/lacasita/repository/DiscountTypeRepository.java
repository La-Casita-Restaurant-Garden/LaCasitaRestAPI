package com.service.lacasita.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.DiscountType;

@Repository
public interface DiscountTypeRepository extends JpaRepository<DiscountType, Long> {

    /** Los que el personal puede elegir, en orden estable para la pantalla. */
    List<DiscountType> findByActivoTrueOrderByNombreAsc();
}
