package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Titulo en la lista de un usuario, con estado, calificacion y resena. */
@Entity
@Table(name = "lista_usuario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "titulo_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "titulo_id")
    private Titulo titulo;

    /** Apunta a estados_lista. */
    @Column(name = "estado_id", nullable = false)
    private Integer estadoId;

    private Integer calificacion;

    @Column(length = 2000)
    private String resena;

    private LocalDateTime creadoEn;

    private LocalDateTime actualizadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now();
    }
}
