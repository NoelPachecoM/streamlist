package com.example.spring_practica.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Activa la revision del token de sesion en todas las rutas /api. */
@Configuration
public class ProteccionConfig implements WebMvcConfigurer {

    private final ProteccionApi proteccion;

    public ProteccionConfig(ProteccionApi proteccion) {
        this.proteccion = proteccion;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(proteccion).addPathPatterns("/api/**");
    }
}
