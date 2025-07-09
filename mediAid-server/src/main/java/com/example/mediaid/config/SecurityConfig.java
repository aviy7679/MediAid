package com.example.mediaid.config;

import com.example.mediaid.security.jwt.JwtFilter;
import com.example.mediaid.constants.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration  //מחלקת קונפיגורציה שמייצרת beans
@EnableWebSecurity //הפעלת אבטחה מותאמת אישית
@EnableMethodSecurity //אישור שימוש ברמת המתודה
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt אלגוריתם לגיבוב סיסמאות עם salt
        // strength מהקבועים אומר שהאלגוריתם יעבור 2^12 איטרציות לחיזוק הגיבוב נגד מתקפות brute force
        return new BCryptPasswordEncoder(SecurityConstants.BCRYPT_STRENGTH);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(SecurityConstants.CORS_ALLOWED_ORIGINS));
        configuration.setAllowedMethods(Arrays.asList(SecurityConstants.CORS_ALLOWED_METHODS));
        configuration.setAllowedHeaders(Arrays.asList(SecurityConstants.CORS_ALLOWED_HEADERS));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(SecurityConstants.CORS_MAX_AGE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}