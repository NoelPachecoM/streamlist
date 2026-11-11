package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Forma en que un titulo esta disponible en una plataforma: suscripcion, renta, compra... */
@Entity
@Table(name = "tipos_disponibilidad")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String clave;

    @Column(nullable = false)
    private String etiqueta;
}
