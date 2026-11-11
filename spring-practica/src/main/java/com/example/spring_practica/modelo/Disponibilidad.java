package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Donde se puede ver un titulo en un pais: plataforma + tipo (suscripcion, renta...). */
@Entity
@Table(name = "disponibilidad",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"titulo_pais_id", "plataforma_id", "tipo_disponibilidad_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "titulo_pais_id")
    private TituloPais tituloPais;

    @Column(name = "plataforma_id", nullable = false)
    private Integer plataformaId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_disponibilidad_id")
    private TipoDisponibilidad tipoDisponibilidad;
}
