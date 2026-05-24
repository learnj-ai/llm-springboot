package com.techcorp.assistant.module05.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP-layer security configuration.
 *
 * <p>This module focuses on <b>LLM-side</b> security (prompt-injection defense,
 * PII masking, output validation, document access control). HTTP authentication
 * is not what's being demonstrated, so we open up the endpoints so the chapter's
 * curl examples work as published.
 *
 * <p>Without this bean, Spring Security autoconfig (pulled in transitively by
 * {@code spring-boot-starter-security}) installs HTTP Basic + a randomly
 * generated dev password on every endpoint, which makes every curl in the
 * tutorial return 401.
 *
 * <p>If you are adapting this module for production, replace this with a real
 * authentication filter chain (JWT, OAuth2 resource server, mTLS, …) — the
 * RAG-layer guardrails in {@code SecureRAGController} sit <i>above</i> whatever
 * HTTP authentication you wire in here.
 */
@Configuration
public class HttpSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
        return http.build();
    }
}
