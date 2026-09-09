package com.antigravity.ui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity in APIs, but in production, keep it for Thymeleaf forms.
            .authorizeHttpRequests(authz -> authz
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                // Admin / Ingestion endpoints
                .requestMatchers("/api/ingest/**", "/api/ingest", "/api/clear", "/admin/**").hasRole("GESTIONNAIRE")
                // General authenticated endpoints
                .requestMatchers("/", "/api/chat").hasAnyRole("GESTIONNAIRE", "UTILISATEUR")
                // Any other request requires authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username("admin")
            .password("{noop}admin123")
            .roles("GESTIONNAIRE")
            .build();

        UserDetails user = User.builder()
            .username("user")
            .password("{noop}user123")
            .roles("UTILISATEUR")
            .build();

        return new InMemoryUserDetailsManager(admin, user);
    }
}
