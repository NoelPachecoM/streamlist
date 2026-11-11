package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SesionRepositorio extends JpaRepository<Sesion, Integer> {

    Optional<Sesion> findByTokenHash(String tokenHash);

    void deleteByUsuarioId(Integer usuarioId);

    void deleteByExpiraEnBefore(LocalDateTime fecha);
}
