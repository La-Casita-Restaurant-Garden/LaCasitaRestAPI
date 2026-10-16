package com.service.lacasita.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.EmailTemplate;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    /** Las que el personal puede elegir, en orden estable para la pantalla. */
    List<EmailTemplate> findByActivaTrueOrderByNombreAsc();
}
