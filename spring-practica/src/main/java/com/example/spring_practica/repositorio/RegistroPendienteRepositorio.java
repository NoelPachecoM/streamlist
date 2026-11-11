package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.RegistroPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroPendienteRepositorio extends JpaRepository<RegistroPendiente, Integer> {

    List<RegistroPendiente> findByCorreo(String correo);
}
