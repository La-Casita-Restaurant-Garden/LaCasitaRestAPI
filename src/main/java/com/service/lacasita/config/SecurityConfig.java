package com.service.lacasita.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import com.service.lacasita.ratelimit.RateLimitFilter;
import com.service.lacasita.utils.JwtAuthFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private static final RequestMatcher SWAGGER_MATCHER = new OrRequestMatcher(
                        new AntPathRequestMatcher("/docs/**"),
                        new AntPathRequestMatcher("/swagger-ui/**"),
                        new AntPathRequestMatcher("/swagger-ui.html"),
                        new AntPathRequestMatcher("/v3/api-docs/**"));

        @Autowired
        private JwtAuthFilter jwtAuthFilter;

        @Autowired
        private RateLimitFilter rateLimitFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(csrf -> csrf.disable())
                                .cors(withDefaults())
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/auth/login",
                                                                "/auth/health",
                                                                "/auth/register",
                                                                "/auth/refresh",
                                                                "/ws/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(unauthorizedEntryPoint())
                                                .accessDeniedHandler(forbiddenHandler()))
                                .headers(headers -> headers
                                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                                 new NegatedRequestMatcher(SWAGGER_MATCHER),
                                                 new ContentSecurityPolicyHeaderWriter("default-src 'none'; frame-ancestors 'none'"))))
                                .addFilterBefore(rateLimitFilter, SecurityContextHolderFilter.class)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        /**
         * RateLimitFilter
         */
        @Bean
        public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
                FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
                registration.setEnabled(false);
                return registration;
        }

        @Bean
        public AuthenticationEntryPoint unauthorizedEntryPoint() {
                return (request, response, authException) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("""
                                        {"body":null,"status":{"code":401,"message":"Unauthenticated or expired token"}}""");
                };
        }

        /** Autenticado pero sin permisos -> 403, en el mismo envoltorio Response. */
        @Bean
        public AccessDeniedHandler forbiddenHandler() {
                return (request, response, accessDeniedException) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("""
                                        {"body":null,"status":{"code":403,"message":"Unauthorized"}}""");
                };
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
