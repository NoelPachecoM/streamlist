package com.example.spring_practica.modelo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Token o codigo que se manda por correo para confirmar una solicitud ARCO
 * (olvide mi password y cancelar suscripcion).
 */
@Entity
@Table(name = "codigos_verificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Si se borra la solicitud, se borran tambien sus codigos
    @ManyToOne
    @JoinColumn(name = "solicitud_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SolicitudArco solicitud;

    @Column(nullable = false, length = 100)
    private String codigo;

    // 'RECUPERAR_PASSWORD', 'CANCELAR_SUSCRIPCION', 'CAMBIAR_CORREO_ACTUAL' o 'CAMBIAR_CORREO_NUEVO'
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime expiracion;

    @Column(nullable = false)
    private Boolean usado = false;

    // Solo para el cambio de correo: el correo nuevo que se esta confirmando
    @Column(name = "dato_nuevo", length = 100)
    private String datoNuevo;

    // Intentos fallidos al escribir el codigo (null cuenta como 0)
    private Integer intentos;
}
