package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Relacion titulo - pais (en que paises se consulto la disponibilidad). */
@Entity
@Table(name = "titulo_pais",
        uniqueConstraints = @UniqueConstraint(columnNames = {"titulo_id", "pais_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TituloPais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "titulo_id")
    private Titulo titulo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pais_id")
    private Pais pais;

    private LocalDateTime consultadoEn;

    @PrePersist
    void marcarFecha() {
        if (consultadoEn == null) consultadoEn = LocalDateTime.now();
    }
}
