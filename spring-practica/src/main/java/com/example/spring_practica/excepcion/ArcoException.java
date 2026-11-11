package com.example.spring_practica.excepcion;

import org.springframework.http.HttpStatus;

public class ArcoException extends RuntimeException {

    private final HttpStatus estado;

    public ArcoException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
