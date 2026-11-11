package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Pelicula o serie. tipoId apunta a tipos_titulo. */
@Entity
@Table(name = "titulos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tmdb_id", "tipo_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Titulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tmdb_id")
    private Integer tmdbId;

    @Column(name = "tipo_id", nullable = false)
    private Integer tipoId;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 4000)
    private String sinopsis;

    private Integer anio;

    private Double calificacionTmdb;

    private String imagenPoster;

    private String imagenFondo;

    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    void marcarFecha() {
        actualizadoEn = LocalDateTime.now();
    }
}
