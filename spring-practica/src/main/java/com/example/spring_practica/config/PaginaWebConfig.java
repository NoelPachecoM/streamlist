package com.example.spring_practica.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Spring tambien sirve la pagina de Angular ya compilada (carpeta src/main/resources/static,
 * se llena con "npm run build:spring"). Asi, en cualquier computadora de la red basta con abrir
 * http://<ip-de-tu-compu> (o http://noel.local) y se ve la pagina completa, sin ng serve.
 *
 * Si alguien abre directo una ruta del Angular (ej. /mi-lista o /titulo/serie/60735), no existe
 * un archivo con ese nombre: se regresa index.html y el Angular muestra la pagina correcta.
 * Las rutas /api siguen yendo a los controladores.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE) // va antes que la configuracion de archivos estaticos de Spring Boot
public class PaginaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String ruta, Resource carpeta) throws IOException {
                        Resource archivo = carpeta.createRelative(ruta);
                        if (archivo.exists() && archivo.isReadable()) {
                            return archivo;
                        }
                        // Una ruta de la API que no existe debe dar 404, no la pagina
                        if (ruta.startsWith("api/") || ruta.equals("api")) {
                            return null;
                        }
                        Resource indice = new ClassPathResource("/static/index.html");
                        return indice.exists() ? indice : null;
                    }
                });
    }
}
