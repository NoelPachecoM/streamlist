package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "plataformas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Integer tmdbId;

    @Column(nullable = false)
    private String nombre;

    private String pais;

    @Column(name = "precio_mensual")
    private Double precioMensual;
}