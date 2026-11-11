package com.example.spring_practica.repositorio;

import com.example.spring_practica.modelo.CacheTmdb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CacheTmdbRepositorio extends JpaRepository<CacheTmdb, Integer> {

    Optional<CacheTmdb> findByClave(String clave);
}
