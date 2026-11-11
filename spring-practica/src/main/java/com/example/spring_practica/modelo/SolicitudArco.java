package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_arco")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudArco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // 'ACCESO', 'RECTIFICACION', 'CANCELACION', 'OPOSICION'
    @Column(name = "tipo_derecho", nullable = false, length = 20)
    private String tipoDerecho;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detalles;

    // 'PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'RECHAZADO'
    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_solicitud", updatable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}