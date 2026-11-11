package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "personas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Integer tmdbId;

    @Column(nullable = false)
    private String nombre;

    private String sexo;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    private String nacionalidad;
}