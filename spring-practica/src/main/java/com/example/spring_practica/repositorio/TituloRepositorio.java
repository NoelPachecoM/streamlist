package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Titulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TituloRepositorio extends JpaRepository<Titulo, Integer> {

    boolean existsByTmdbIdAndTipoId(Integer tmdbId, Integer tipoId);

    Optional<Titulo> findByTmdbIdAndTipoId(Integer tmdbId, Integer tipoId);

    Page<Titulo> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
