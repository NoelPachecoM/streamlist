package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sesion iniciada. El token se le da al navegador al iniciar sesion; aqui solo se guarda su
 * huella SHA-256 (token_hash), asi que aunque alguien vea esta tabla no puede usar los tokens.
 */
@Entity
@Table(name = "sesiones")
@Data
@NoArgsConstructor
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    private LocalDateTime creadaEn;

    private LocalDateTime expiraEn;
}
