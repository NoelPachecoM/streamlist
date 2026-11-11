package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Genero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneroRepositorio extends JpaRepository<Genero, Integer> {
}