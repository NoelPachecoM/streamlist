package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Pais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaisRepositorio extends JpaRepository<Pais, Integer> {

    boolean existsByCodigo(String codigo);

    Optional<Pais> findByCodigo(String codigo);
}
