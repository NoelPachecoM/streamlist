package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "generos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Genero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Integer tmdbId;

    @Column(nullable = false)
    private String nombre;
}