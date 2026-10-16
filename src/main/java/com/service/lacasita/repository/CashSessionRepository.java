package com.service.lacasita.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.CashSession;

@Repository
public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

 
    List<CashSession> findByEstadoOrderByAbiertaAtDesc(String estado);

    Page<CashSession> findAllByOrderByAbiertaAtDesc(Pageable pageable);
}
