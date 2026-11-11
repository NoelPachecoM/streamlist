package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Disponibilidad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisponibilidadRepositorio extends JpaRepository<Disponibilidad, Integer> {

    boolean existsByTituloPais_IdAndPlataformaIdAndTipoDisponibilidad_Id(
            Integer tituloPaisId, Integer plataformaId, Integer tipoDisponibilidadId);

    Optional<Disponibilidad> findByTituloPais_IdAndPlataformaIdAndTipoDisponibilidad_Id(
            Integer tituloPaisId, Integer plataformaId, Integer tipoDisponibilidadId);

    Page<Disponibilidad> findByTituloPais_Id(Integer tituloPaisId, Pageable pageable);
}
