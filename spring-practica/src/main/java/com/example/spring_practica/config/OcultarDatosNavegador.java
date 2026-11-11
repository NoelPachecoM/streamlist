package com.example.spring_practica.config;

import com.example.spring_practica.dto.ArcoRespuesta;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lo que ve el navegador (pestana Network de las herramientas de desarrollador) NO debe traer
 * datos de prueba: ni la password, ni los codigos de verificacion.
 *
 * Los navegadores mandan el encabezado "Sec-Fetch-Site" en todas sus peticiones (y "Origin" en
 * las que van a otro sitio); Postman no manda ninguno de los dos. Por eso:
 *   - Desde Postman todo sigue igual que lo reviso el profesor
 *     (password en GET /api/arco/datos y codigoDesarrollo con arco.devolver-codigo=true).
 *   - Desde el navegador se quitan "password" y "codigoDesarrollo" de la respuesta, tanto con
 *     ng serve (localhost:4200) como cuando Spring sirve la pagina (misma direccion).
 */
@RestControllerAdvice
public class OcultarDatosNavegador implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        boolean navegador = request.getHeaders().getFirst("Origin") != null
                || request.getHeaders().getFirst("Sec-Fetch-Site") != null;
        if (body == null || !navegador) {
            return body;
        }
        if (body instanceof ArcoRespuesta respuesta) {
            return new ArcoRespuesta(respuesta.getMensaje(), respuesta.getSolicitudId(), null);
        }
        if (body instanceof Map<?, ?> mapa && mapa.containsKey("password")) {
            Map<Object, Object> limpio = new LinkedHashMap<>(mapa);
            limpio.remove("password");
            return limpio;
        }
        return body;
    }
}
