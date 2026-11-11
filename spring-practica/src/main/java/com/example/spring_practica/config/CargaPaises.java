package com.example.spring_practica.config;

import com.example.spring_practica.modelo.Pais;
import com.example.spring_practica.repositorio.PaisRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Datos de ejemplo de paises. Solo se cargan si la tabla esta vacia. */
@Component
public class CargaPaises implements CommandLineRunner {

    private final PaisRepositorio repo;

    public CargaPaises(PaisRepositorio repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;
        repo.saveAll(List.of(
                new Pais(null, "MX", "Mexico", "es-MX", "Latinoamerica"),
                new Pais(null, "US", "Estados Unidos", "en-US", "Norteamerica"),
                new Pais(null, "ES", "Espana", "es-ES", "Europa"),
                new Pais(null, "AR", "Argentina", "es-AR", "Latinoamerica"),
                new Pais(null, "CO", "Colombia", "es-CO", "Latinoamerica"),
                new Pais(null, "CL", "Chile", "es-CL", "Latinoamerica"),
                new Pais(null, "BR", "Brasil", "pt-BR", "Latinoamerica"),
                new Pais(null, "FR", "Francia", "fr-FR", "Europa"),
                new Pais(null, "JP", "Japon", "ja-JP", "Asia"),
                new Pais(null, "KR", "Corea del Sur", "ko-KR", "Asia")));
        System.out.println("Carga de paises terminada.");
    }
}
