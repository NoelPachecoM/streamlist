package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.modelo.TituloGenero;
import com.example.spring_practica.repositorio.TituloGeneroRepositorio;
import com.example.spring_practica.repositorio.TituloRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/titulo-genero")
public class TituloGeneroControlador {

    private final TituloGeneroRepositorio repositorio;
    private final TituloRepositorio titulos;

    public TituloGeneroControlador(TituloGeneroRepositorio repositorio, TituloRepositorio titulos) {
        this.repositorio = repositorio;
        this.titulos = titulos;
    }

    // GET /api/titulo-genero?page=0&size=20&tituloId=3   (tituloId es opcional)
    @GetMapping
    public Page<TituloGenero> listar(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) Integer tituloId) {
        PageRequest pagina = PageRequest.of(page, size);
        if (tituloId != null) {
            return repositorio.findByTitulo_Id(tituloId, pagina);
        }
        return repositorio.findAll(pagina);
    }

    // GET /api/titulo-genero/5
    @GetMapping("/{id}")
    public ResponseEntity<TituloGenero> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/titulo-genero   {"titulo":{"id":1},"generoId":2}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TituloGenero nuevo) {
        if (nuevo.getTitulo() == null || nuevo.getTitulo().getId() == null || nuevo.getGeneroId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan titulo.id y/o generoId"));
        }
        Titulo titulo = titulos.findById(nuevo.getTitulo().getId()).orElse(null);
        if (titulo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El titulo no existe"));
        }
        if (repositorio.existsByTitulo_IdAndGeneroId(titulo.getId(), nuevo.getGeneroId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese titulo ya tiene ese genero"));
        }
        nuevo.setId(null);
        nuevo.setTitulo(titulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/titulo-genero/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody TituloGenero datos) {
        TituloGenero actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        Titulo titulo = actual.getTitulo();
        if (datos.getTitulo() != null && datos.getTitulo().getId() != null) {
            titulo = titulos.findById(datos.getTitulo().getId()).orElse(null);
            if (titulo == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El titulo no existe"));
            }
        }
        Integer generoId = datos.getGeneroId() != null ? datos.getGeneroId() : actual.getGeneroId();
        final Titulo tituloFinal = titulo;
        boolean repetido = repositorio.findByTitulo_IdAndGeneroId(tituloFinal.getId(), generoId)
                .filter(otro -> !otro.getId().equals(id))
                .isPresent();
        if (repetido) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese titulo ya tiene ese genero"));
        }
        actual.setTitulo(tituloFinal);
        actual.setGeneroId(generoId);
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/titulo-genero/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
