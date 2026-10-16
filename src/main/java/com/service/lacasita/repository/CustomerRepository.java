package com.service.lacasita.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.service.lacasita.entity.Customer;



@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /*
    * Method to get all customers
    */
   Page<Customer> findAll(Pageable pageable);

   /*
   * Method to get customer by email
   */
   Optional<Customer> findByEmail(String email);

   /** Por nombre, apellido, teléfono o correo — para el buscador del selector de clientes. */
   @Query("""
           SELECT c FROM Customer c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.last_name) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.email) LIKE LOWER(CONCAT('%', :termino, '%'))
           """)
   Page<Customer> buscar(@Param("termino") String termino, Pageable pageable);
}
