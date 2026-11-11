package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.BitacoraAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BitacoraAdminRepositorio extends JpaRepository<BitacoraAdmin, Integer> {

    // Los 200 registros mas recientes
    List<BitacoraAdmin> findTop200ByOrderByFechaDesc();
}
