package com.example.mediaid.utils;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration  //מחלקת קונפיגורציה שמייצרת beans
@EnableWebSecurity //הפעלת אבטחה מותאמת אישית
@EnableMethodSecurity //אישור שימוש ברמת המתודה
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt אלגוריתם לגיבוב סיסמאות עם salt
        // strength=12 אומר שהאלגוריתם יעבור 2^12 איטרציות לחיזוק הגיבוב נגד מתקפות brute force
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration(); //אובייקט הגדרות של CORS
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // מאיפה מותר לגשת
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));  //מה מותר לעשות
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With")); //HTTP headers שמותר לקבל
        configuration.setAllowCredentials(true);  //בקשות עם קוקיז וטוקנים
        configuration.setMaxAge(3600L); // מטמון של בקשות preflight למשך שעה

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) //הגדרות הCORS מהפונקציה למעלה
                .csrf(AbstractHttpConfigurer::disable)  //לא רלוונטי בAPI מתקדמים
                .authorizeHttpRequests(auth -> auth
                        //מגדיר נתיבים שלא דורשים התחברות
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/signUp").permitAll()  // הוספת הרשאה לנתיב ההרשמה
                        .requestMatchers("/logIn").permitAll()   // הוספת הרשאה לנתיב ההתחברות
                        .anyRequest().authenticated() //כל השאר - חייב החברות
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(httpBasic -> {}); // או אל תשתמש בכלל אם זה לא נדרש

        return http.build();
    }
    }