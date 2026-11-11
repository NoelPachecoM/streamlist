package com.example.spring_practica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Paginas que pueden llamar a esta API desde el navegador. La lista viene de cors.origenes
 * (variable CORS_ORIGENES en Render), por ejemplo: https://paginanoel.online,https://www.paginanoel.online
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(@Value("${cors.origenes}") String origenes) {
        String[] lista = Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(lista)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
