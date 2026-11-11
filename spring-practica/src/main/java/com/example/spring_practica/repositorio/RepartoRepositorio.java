package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Reparto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepartoRepositorio extends JpaRepository<Reparto, Integer> {

    Page<Reparto> findByTitulo_Id(Integer tituloId, Pageable pageable);
}
