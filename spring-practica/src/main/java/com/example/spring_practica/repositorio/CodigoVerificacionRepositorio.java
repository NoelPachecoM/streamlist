package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodigoVerificacionRepositorio extends JpaRepository<CodigoVerificacion, Integer> {

    // Ultimo codigo todavia sin usar de una solicitud
    Optional<CodigoVerificacion> findFirstBySolicitudIdAndTipoAndUsadoFalseOrderByIdDesc(
            Integer solicitudId, String tipo);
}
