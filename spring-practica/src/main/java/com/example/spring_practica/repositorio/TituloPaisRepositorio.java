package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.TituloPais;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TituloPaisRepositorio extends JpaRepository<TituloPais, Integer> {

    boolean existsByTitulo_IdAndPais_Id(Integer tituloId, Integer paisId);

    Optional<TituloPais> findByTitulo_IdAndPais_Id(Integer tituloId, Integer paisId);

    Page<TituloPais> findByTitulo_Id(Integer tituloId, Pageable pageable);
}
