package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Bitacora del panel de Administracion: que hizo cada administrador y cuando. */
@Entity
@Table(name = "bitacora_admin")
@Data
@NoArgsConstructor
public class BitacoraAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_id", nullable = false)
    private Integer adminId;

    // Se guarda el nombre del momento, por si despues el administrador cambia de nombre o se borra
    @Column(name = "admin_nombre", length = 100)
    private String adminNombre;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(length = 2000)
    private String detalle;

    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) fecha = LocalDateTime.now();
    }
}
