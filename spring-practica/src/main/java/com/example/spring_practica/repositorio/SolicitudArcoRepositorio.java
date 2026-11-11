package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.SolicitudArco;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudArcoRepositorio extends JpaRepository<SolicitudArco, Integer> {

    void deleteByUsuarioId(Integer usuarioId);
}
