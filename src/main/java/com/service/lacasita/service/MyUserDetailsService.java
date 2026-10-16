package com.service.lacasita.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.service.lacasita.repository.UserRespository;
import com.service.lacasita.entity.UserModel;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRespository userRespository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserModel userEntity = userRespository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new User(
                userEntity.getEmail(),
                userEntity.getPassword(),
                Collections.emptyList()
        );
    }
}
