package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro a medias: el usuario ya mando sus datos, pero todavia no confirma
 * el codigo que llego a su correo. Hasta entonces NO existe en la tabla usuarios.
 */
@Entity
@Table(name = "registros_pendientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime expiracion;

    // Cuando se mando el ultimo codigo (para esperar 60 segundos antes de reenviar)
    @Column(name = "ultimo_envio", nullable = false)
    private LocalDateTime ultimoEnvio;

    @Column(nullable = false)
    private Integer intentos = 0;
}
