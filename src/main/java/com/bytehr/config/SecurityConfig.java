package com.bytehr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration.
 * <p>
 * Teams sends signed JWT tokens on the Authorization header.
 * For a production deployment, validate the Bot Framework token here.
 * For the MVP, CSRF is disabled (Teams sends requests from its servers, not a browser context).
 * <p>
 * Swagger / OpenAPI paths are explicitly opened using {@link AntPathRequestMatcher} to avoid
 * ambiguity when both spring-webmvc and spring-webflux are on the classpath (WebClient dep).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Application endpoints
                .requestMatchers(
                    new AntPathRequestMatcher("/api/messages"),
                    new AntPathRequestMatcher("/api/sync"),
                    new AntPathRequestMatcher("/api/chat"),
                    new AntPathRequestMatcher("/api/admin/**")
                ).permitAll()
                // Actuator
                .requestMatchers(
                    new AntPathRequestMatcher("/actuator/health"),
                    new AntPathRequestMatcher("/actuator/info")
                ).permitAll()
                // Swagger UI — entry point, all UI assets, OAuth2 redirect
                .requestMatchers(
                    new AntPathRequestMatcher("/swagger-ui.html"),
                    new AntPathRequestMatcher("/swagger-ui/**")
                ).permitAll()
                // OpenAPI spec — JSON (exact + sub-paths), YAML, and swagger-config
                .requestMatchers(
                    new AntPathRequestMatcher("/v3/api-docs"),
                    new AntPathRequestMatcher("/v3/api-docs/**"),
                    new AntPathRequestMatcher("/v3/api-docs.yaml")
                ).permitAll()
                // Webjars — swagger-ui JS/CSS assets served from classpath
                .requestMatchers(
                    new AntPathRequestMatcher("/webjars/**")
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
