package com.example.spring_practica.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos para registrarse (nombre, correo, password, telefono), iniciar sesion (correo, password)
 * o confirmar el registro (solicitudId, codigo).
 */
@Data
@NoArgsConstructor
public class AuthPeticion {
    private String nombre;
    private String correo;
    private String password;
    private String telefono;
    private Integer solicitudId;
    private String codigo;
}
