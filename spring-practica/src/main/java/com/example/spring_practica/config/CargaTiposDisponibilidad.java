package com.example.spring_practica.config;

import com.example.spring_practica.modelo.TipoDisponibilidad;
import com.example.spring_practica.repositorio.TipoDisponibilidadRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Datos de ejemplo de tipos de disponibilidad. Solo se cargan si la tabla esta vacia. */
@Component
public class CargaTiposDisponibilidad implements CommandLineRunner {

    private final TipoDisponibilidadRepositorio repo;

    public CargaTiposDisponibilidad(TipoDisponibilidadRepositorio repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;
        repo.saveAll(List.of(
                new TipoDisponibilidad(null, "suscripcion", "Suscripcion"),
                new TipoDisponibilidad(null, "renta", "Renta"),
                new TipoDisponibilidad(null, "compra", "Compra"),
                new TipoDisponibilidad(null, "gratis", "Gratis")));
        System.out.println("Carga de tipos de disponibilidad terminada.");
    }
}
