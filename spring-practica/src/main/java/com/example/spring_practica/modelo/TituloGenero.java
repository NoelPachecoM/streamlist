package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Relacion titulo - genero. generoId apunta a la tabla generos. */
@Entity
@Table(name = "titulo_genero",
        uniqueConstraints = @UniqueConstraint(columnNames = {"titulo_id", "genero_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TituloGenero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "titulo_id")
    private Titulo titulo;

    @Column(name = "genero_id", nullable = false)
    private Integer generoId;
}
