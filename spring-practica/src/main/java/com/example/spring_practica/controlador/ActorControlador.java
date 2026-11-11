package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Actor;
import com.example.spring_practica.repositorio.ActorRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/actores")
public class ActorControlador {

    private final ActorRepositorio repositorio;

    public ActorControlador(ActorRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/actores?page=0&size=20
    @GetMapping
    public Page<Actor> listar(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/actores/5
    @GetMapping("/{id}")
    public ResponseEntity<Actor> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/actores   {"nombre":"Keanu Reeves","direccion":"Los Angeles","telefono":"5555555555","genero":"Masculino","pais":"Canada"}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Actor nuevo) {
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta el nombre"));
        }
        nuevo.setIdActor(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/actores/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Actor datos) {
        Actor actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        if (datos.getNombre() != null && !datos.getNombre().isBlank()) actual.setNombre(datos.getNombre());
        if (datos.getDireccion() != null) actual.setDireccion(datos.getDireccion());
        if (datos.getTelefono() != null) actual.setTelefono(datos.getTelefono());
        if (datos.getGenero() != null) actual.setGenero(datos.getGenero());
        if (datos.getPais() != null) actual.setPais(datos.getPais());
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/actores/5
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
                    .body(Map.of("error", "El actor esta en un reparto. Quitalo del reparto primero."));
        }
        return ResponseEntity.noContent().build();
    }
}
