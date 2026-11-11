package com.example.spring_practica.config;

import com.example.spring_practica.modelo.EstadoLista;
import com.example.spring_practica.modelo.ListaUsuario;
import com.example.spring_practica.repositorio.EstadoListaRepositorio;
import com.example.spring_practica.repositorio.ListaUsuarioRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * La pagina ya solo usa 3 estados: pendiente ("Por ver"), viendo y completado.
 * Al arrancar, las filas de lista_usuario que quedaron en "en_pausa" o "abandonado" pasan a "viendo".
 * La tabla estados_lista NO se toca (sus 5 filas y su CRUD siguen igual para Postman).
 */
@Component
public class AjusteEstadosLista implements CommandLineRunner {

    private final EstadoListaRepositorio estados;
    private final ListaUsuarioRepositorio lista;

    public AjusteEstadosLista(EstadoListaRepositorio estados, ListaUsuarioRepositorio lista) {
        this.estados = estados;
        this.lista = lista;
    }

    @Override
    public void run(String... args) {
        EstadoLista viendo = estados.findByClave("viendo").orElse(null);
        if (viendo == null) return;
        List<Integer> viejos = new ArrayList<>();
        estados.findByClave("en_pausa").ifPresent(e -> viejos.add(e.getId()));
        estados.findByClave("abandonado").ifPresent(e -> viejos.add(e.getId()));
        if (viejos.isEmpty()) return;

        List<ListaUsuario> filas = lista.findByEstadoIdIn(viejos);
        for (ListaUsuario fila : filas) {
            fila.setEstadoId(viendo.getId());
        }
        if (!filas.isEmpty()) {
            lista.saveAll(filas);
            System.out.println("Estados de lista ajustados: " + filas.size() + " fila(s) pasaron a 'viendo'.");
        }
    }
}
