package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Genero;
import com.example.spring_practica.repositorio.GeneroRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generos")
public class GeneroControlador {

    private final GeneroRepositorio repositorio;

    public GeneroControlador(GeneroRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/generos?page=0&size=20
    @GetMapping
    public Page<Genero> listar(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/generos/5
    @GetMapping("/{id}")
    public ResponseEntity<Genero> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/generos
    @PostMapping
    public ResponseEntity<Genero> crear(@RequestBody Genero nuevo) {
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/generos/5
    @PutMapping("/{id}")
    public ResponseEntity<Genero> actualizar(@PathVariable Integer id, @RequestBody Genero datos) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        datos.setId(id);
        return ResponseEntity.ok(repositorio.save(datos));
    }

    // DELETE /api/generos/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}