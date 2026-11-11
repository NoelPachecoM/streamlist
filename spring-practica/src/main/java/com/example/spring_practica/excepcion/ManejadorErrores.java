package com.example.spring_practica.excepcion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Convierte las ArcoException en respuestas JSON como {"error": "mensaje"}. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ArcoException.class)
    public ResponseEntity<Map<String, String>> manejarArco(ArcoException e) {
        return ResponseEntity.status(e.getEstado()).body(Map.of("error", e.getMessage()));
    }
}
