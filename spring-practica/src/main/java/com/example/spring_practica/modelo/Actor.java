package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Actor (tabla "actor" del Excel). */
@Entity
@Table(name = "actor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actor")
    private Integer idActor;

    @Column(nullable = false)
    private String nombre;

    private String direccion;

    private String telefono;

    private String genero;

    private String pais;
}
