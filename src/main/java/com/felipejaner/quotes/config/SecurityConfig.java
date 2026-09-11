package com.felipejaner.quotes.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipejaner.quotes.api.ApiError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration
public class SecurityConfig {
    @Bean UserDetailsService users(@Value("${quotes.auth.username}") String username, @Value("${quotes.auth.password}") String password) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
            .password(new BCryptPasswordEncoder().encode(password)).roles("REVIEWER").build());
    }
    @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper, @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, exception) -> {
            response.setStatus(401); response.setContentType("application/json");
            // Deliberately omit WWW-Authenticate to avoid the browser's native Basic dialog.
            mapper.writeValue(response.getOutputStream(), ApiError.of("UNAUTHORIZED", "Sign in with the configured API credentials."));
        };
        return http.cors(c -> c.configurationSource(corsSource)).csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll().anyRequest().authenticated())
            .httpBasic(b -> b.authenticationEntryPoint(entryPoint))
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler((request, response, ex) -> {
                response.setStatus(403); response.setContentType("application/json");
                mapper.writeValue(response.getOutputStream(), ApiError.of("FORBIDDEN", "You cannot perform this action."));
            })).build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${quotes.cors.allowed-origin}") String origin) {
        var config = new CorsConfiguration(); config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config); return source;
    }
}
