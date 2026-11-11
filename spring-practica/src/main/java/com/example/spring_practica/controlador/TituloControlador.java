package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.repositorio.TituloRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/titulos")
public class TituloControlador {

    private final TituloRepositorio repositorio;

    public TituloControlador(TituloRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/titulos?page=0&size=20&q=inception   (q es opcional: busca por nombre)
    @GetMapping
    public Page<Titulo> listar(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String q) {
        PageRequest pagina = PageRequest.of(page, size);
        if (q != null && !q.isBlank()) {
            return repositorio.findByNombreContainingIgnoreCase(q.trim(), pagina);
        }
        return repositorio.findAll(pagina);
    }

    // GET /api/titulos/5
    @GetMapping("/{id}")
    public ResponseEntity<Titulo> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/titulos
    // {"tmdbId":603,"tipoId":1,"nombre":"Matrix","sinopsis":"...","anio":1999,"calificacionTmdb":8.2}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Titulo nuevo) {
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank() || nuevo.getTipoId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan nombre y/o tipoId"));
        }
        if (nuevo.getTmdbId() != null && repositorio.existsByTmdbIdAndTipoId(nuevo.getTmdbId(), nuevo.getTipoId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un titulo con ese tmdbId y tipoId"));
        }
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/titulos/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Titulo datos) {
        Titulo actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        Integer nuevoTmdb = datos.getTmdbId() != null ? datos.getTmdbId() : actual.getTmdbId();
        Integer nuevoTipo = datos.getTipoId() != null ? datos.getTipoId() : actual.getTipoId();
        if (nuevoTmdb != null) {
            boolean usadoPorOtro = repositorio.findByTmdbIdAndTipoId(nuevoTmdb, nuevoTipo)
                    .filter(otro -> !otro.getId().equals(id))
                    .isPresent();
            if (usadoPorOtro) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Ya existe otro titulo con ese tmdbId y tipoId"));
            }
        }
        actual.setTmdbId(nuevoTmdb);
        actual.setTipoId(nuevoTipo);
        if (datos.getNombre() != null && !datos.getNombre().isBlank()) actual.setNombre(datos.getNombre());
        if (datos.getSinopsis() != null) actual.setSinopsis(datos.getSinopsis());
        if (datos.getAnio() != null) actual.setAnio(datos.getAnio());
        if (datos.getCalificacionTmdb() != null) actual.setCalificacionTmdb(datos.getCalificacionTmdb());
        if (datos.getImagenPoster() != null) actual.setImagenPoster(datos.getImagenPoster());
        if (datos.getImagenFondo() != null) actual.setImagenFondo(datos.getImagenFondo());
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/titulos/5
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            repositorio.deleteById(id);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El titulo esta en uso (lista, favoritos, reparto, generos o paises). Borra esas relaciones primero."));
        }
        return ResponseEntity.noContent().build();
    }
}
