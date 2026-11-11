package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Favorito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoritoRepositorio extends JpaRepository<Favorito, Integer> {

    boolean existsByUsuarioIdAndTitulo_Id(Integer usuarioId, Integer tituloId);

    Optional<Favorito> findByUsuarioIdAndTitulo_Id(Integer usuarioId, Integer tituloId);

    Page<Favorito> findByUsuarioId(Integer usuarioId, Pageable pageable);
}
