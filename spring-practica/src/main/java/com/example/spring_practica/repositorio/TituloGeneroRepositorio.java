package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.TituloGenero;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TituloGeneroRepositorio extends JpaRepository<TituloGenero, Integer> {

    boolean existsByTitulo_IdAndGeneroId(Integer tituloId, Integer generoId);

    Optional<TituloGenero> findByTitulo_IdAndGeneroId(Integer tituloId, Integer generoId);

    Page<TituloGenero> findByTitulo_Id(Integer tituloId, Pageable pageable);
}
