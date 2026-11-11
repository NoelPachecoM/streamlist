package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Titulo marcado como favorito por un usuario. */
@Entity
@Table(name = "favoritos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "titulo_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "titulo_id")
    private Titulo titulo;

    private LocalDateTime creadoEn;

    @PrePersist
    void marcarFecha() {
        creadoEn = LocalDateTime.now();
    }
}
