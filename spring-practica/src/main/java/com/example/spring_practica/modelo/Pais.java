package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pais para la disponibilidad de titulos (codigo ISO de 2 letras, ej. MX, US). */
@Entity
@Table(name = "paises")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String idioma;

    private String region;
}
