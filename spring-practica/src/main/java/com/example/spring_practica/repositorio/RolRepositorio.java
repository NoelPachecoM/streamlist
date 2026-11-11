package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepositorio extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);
}