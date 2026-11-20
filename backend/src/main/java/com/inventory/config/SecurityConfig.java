package com.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.exception.ApiError;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtAuthenticationFilter;
import com.inventory.security.JwtService;
import com.inventory.security.PasswordChangeRequiredFilter;
import com.inventory.security.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           UserDetailsService userDetailsService,
                                           ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Oturum açmanız gerekiyor"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "Bu işlem için yetkiniz yok")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register",
                                "/api/auth/forgot-password", "/error").permitAll()
                        // Sadece yönetici
                        .requestMatchers("/api/users/**", "/api/audit-logs/**", "/api/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/equipment/export", "/api/equipment/import",
                                "/api/equipment/import-template").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/equipment/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/equipment/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/equipment/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/equipment/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/approve",
                                "/api/reservations/*/reject").hasRole("ADMIN")
                        // Diğer her şey için giriş yapmış olmak yeterli
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                // Tek kullanımlık şifreyle girenler yeni şifre belirlemeden başka işlem yapamaz
                .addFilterAfter(new PasswordChangeRequiredFilter(objectMapper), JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // İndirilen dosyanın (Excel / PDF) adını tarayıcının okuyabilmesi için
        config.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper,
                                   int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, message));
    }
}
