package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cache de respuestas de TMDB. Antes de llamar a TMDB, Spring busca aqui la misma consulta;
 * si existe y no ha vencido, la usa y NO gasta una llamada a la API.
 *
 * - clave: la consulta exacta (ruta + parametros), por ejemplo "/tv/60735?append_to_response=...".
 * - descripcion: texto para leer en pgAdmin (ej. "Detalle: Flash").
 * - contenido: la respuesta de TMDB guardada en binario (columna bytea).
 */
@Entity
@Table(name = "cache_tmdb")
@Data
@NoArgsConstructor
public class CacheTmdb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 300)
    private String clave;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false)
    private byte[] contenido;

    private LocalDateTime guardadoEn;

    private LocalDateTime venceEn;
}
