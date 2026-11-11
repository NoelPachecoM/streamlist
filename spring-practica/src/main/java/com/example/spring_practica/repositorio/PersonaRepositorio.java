package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepositorio extends JpaRepository<Persona, Integer> {
}