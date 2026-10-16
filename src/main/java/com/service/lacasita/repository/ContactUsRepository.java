package com.service.lacasita.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.service.lacasita.entity.ContactUs;

@Repository
public interface ContactUsRepository extends JpaRepository<ContactUs, Long>{
    
}
