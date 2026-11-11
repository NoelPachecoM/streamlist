package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reparto de un titulo. Puede ligarse a una persona (personaId) y/o a un actor. */
@Entity
@Table(name = "reparto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reparto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "titulo_id")
    private Titulo titulo;

    @Column(name = "persona_id")
    private Integer personaId;

    private Integer orden;

    private String personaje;

    @ManyToOne
    @JoinColumn(name = "id_actor")
    private Actor actor;
}
