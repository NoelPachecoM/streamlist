package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tipos_titulo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoTitulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String clave;

    @Column(nullable = false)
    private String etiqueta;
}