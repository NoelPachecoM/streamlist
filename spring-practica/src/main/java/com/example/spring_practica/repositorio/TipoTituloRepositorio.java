package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.TipoTitulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoTituloRepositorio extends JpaRepository<TipoTitulo, Integer> {

    Optional<TipoTitulo> findByClave(String clave);
}