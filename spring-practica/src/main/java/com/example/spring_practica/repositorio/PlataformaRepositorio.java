package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.Plataforma;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlataformaRepositorio extends JpaRepository<Plataforma, Integer> {
}