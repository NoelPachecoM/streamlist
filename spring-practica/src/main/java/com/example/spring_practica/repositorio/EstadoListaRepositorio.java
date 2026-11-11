package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.EstadoLista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoListaRepositorio extends JpaRepository<EstadoLista, Integer> {

    Optional<EstadoLista> findByClave(String clave);
}