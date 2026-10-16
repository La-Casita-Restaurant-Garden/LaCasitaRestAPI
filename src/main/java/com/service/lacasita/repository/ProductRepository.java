package com.service.lacasita.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.service.lacasita.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page <Product> findAll(Pageable pageable);

    @Query(value = "SELECT * FROM fnGetProductsInMenu()", nativeQuery = true)
    Page<Product> findAllProductsInMenu(Pageable pageable);

    /** El menú de hoy, sin paginar: lo consume el selector de productos del POS. */
    @Query(value = "SELECT * FROM fnGetProductsInMenu()", nativeQuery = true)
    List<Product> findAllInMenu();
}
