package com.example.spring_practica.controlador;

import com.example.spring_practica.servicio.TmdbServicio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Busqueda de peliculas y series (datos de TMDB).
 *   GET /api/tmdb/buscar?q=batman&pagina=1
 *   GET /api/tmdb/detalle?id=550&tipo=pelicula&pais=MX      (tipo: pelicula | serie)
 *   GET /api/tmdb/tendencias?periodo=semana                 (periodo: semana | dia)
 * Todo pasa por la cache de la tabla cache_tmdb.
 */
@RestController
@RequestMapping("/api/tmdb")
public class TmdbControlador {

    private final TmdbServicio servicio;

    public TmdbControlador(TmdbServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/buscar")
    public Map<String, Object> buscar(@RequestParam("q") String consulta,
                                      @RequestParam(value = "pagina", defaultValue = "1") int pagina) {
        return servicio.buscar(consulta, pagina);
    }

    @GetMapping("/detalle")
    public Map<String, Object> detalle(@RequestParam("id") long id,
                                       @RequestParam("tipo") String tipo,
                                       @RequestParam(value = "pais", defaultValue = "MX") String pais) {
        return servicio.detalle(id, tipo, pais);
    }

    @GetMapping("/tendencias")
    public Map<String, Object> tendencias(@RequestParam(value = "periodo", defaultValue = "semana") String periodo) {
        return servicio.tendencias(periodo);
    }
}
