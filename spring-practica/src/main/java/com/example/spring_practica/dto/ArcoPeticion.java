package com.example.spring_practica.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos que el usuario manda en cualquier operacion ARCO.
 * Cada operacion usa solo los campos que necesita; los demas pueden omitirse.
 */
@Data
@NoArgsConstructor
public class ArcoPeticion {

    // Datos para identificar y verificar al usuario
    private String correo;
    private String nombre;
    private String password;

    // Datos nuevos
    private String passwordNueva;
    private String nombreNuevo;
    private String correoNuevo;
    private String telefonoNuevo;

    // Para confirmar los flujos con correo (olvide password y cancelar)
    private Integer solicitudId;
    private String token;
    private String codigo;
}
