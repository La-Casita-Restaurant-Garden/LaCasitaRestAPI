package com.service.lacasita.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class ActuatorSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain actuatorChain(
            HttpSecurity http,
            ObjectProvider<PasswordEncoder> appEncoder,
            @Value("${ACTUATOR_USER:prometheus}") String user,
            @Value("${ACTUATOR_PASSWORD}") String password) throws Exception {


        PasswordEncoder encoder = appEncoder.getIfAvailable(
                PasswordEncoderFactories::createDelegatingPasswordEncoder);

        var actuatorUsers = new InMemoryUserDetailsManager(
                User.withUsername(user)
                        .password(encoder.encode(password))
                        .roles("ACTUATOR")
                        .build());

        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.to("health")).permitAll()
                .anyRequest().hasRole("ACTUATOR"))
            .userDetailsService(actuatorUsers)
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
