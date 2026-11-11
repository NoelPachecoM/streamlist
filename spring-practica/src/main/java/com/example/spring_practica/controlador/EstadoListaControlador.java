package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.EstadoLista;
import com.example.spring_practica.repositorio.EstadoListaRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estados-lista")
public class EstadoListaControlador {

    private final EstadoListaRepositorio repositorio;

    public EstadoListaControlador(EstadoListaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/estados-lista?page=0&size=20
    @GetMapping
    public Page<EstadoLista> listar(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/estados-lista/5
    @GetMapping("/{id}")
    public ResponseEntity<EstadoLista> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/estados-lista
    @PostMapping
    public ResponseEntity<EstadoLista> crear(@RequestBody EstadoLista nuevo) {
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/estados-lista/5
    @PutMapping("/{id}")
    public ResponseEntity<EstadoLista> actualizar(@PathVariable Integer id, @RequestBody EstadoLista datos) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        datos.setId(id);
        return ResponseEntity.ok(repositorio.save(datos));
    }

    // DELETE /api/estados-lista/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}