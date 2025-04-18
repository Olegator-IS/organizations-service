package com.placeandplay.organizationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1) выключаем CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 2) настраиваем правила доступа
                .authorizeHttpRequests(auth -> auth
                        // Путь регистрации разрешен без аутентификации
                        .requestMatchers("/auth/organization/**").permitAll()

                        // другие пути — открытые
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // Эти — только для авторизованных
                        .requestMatchers("/api/private/events/**").authenticated()

                        // всё остальное — тоже под авторизацию
                        .anyRequest().authenticated()
                )

                // 3) вставляем JWT‑фильтр перед стандартным UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        // 4) «собираем» конфигурацию
        return http.build();
    }

    @Bean
    public SecretKey secretKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // Используйте 128, 192 или 256 бит для безопасности
        return keyGen.generateKey();
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}

