package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Persona;
import com.example.spring_practica.repositorio.PersonaRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personas")
public class PersonaControlador {

    private final PersonaRepositorio repositorio;

    public PersonaControlador(PersonaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/personas?page=0&size=20
    @GetMapping
    public Page<Persona> listar(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/personas/5
    @GetMapping("/{id}")
    public ResponseEntity<Persona> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/personas
    @PostMapping
    public ResponseEntity<Persona> crear(@RequestBody Persona nuevo) {
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/personas/5
    @PutMapping("/{id}")
    public ResponseEntity<Persona> actualizar(@PathVariable Integer id, @RequestBody Persona datos) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        datos.setId(id);
        return ResponseEntity.ok(repositorio.save(datos));
    }

    // DELETE /api/personas/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}