package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.TipoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoDisponibilidadRepositorio extends JpaRepository<TipoDisponibilidad, Integer> {

    boolean existsByClave(String clave);

    Optional<TipoDisponibilidad> findByClave(String clave);
}
