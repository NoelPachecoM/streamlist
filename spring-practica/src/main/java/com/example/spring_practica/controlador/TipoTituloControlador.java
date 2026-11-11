package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.TipoTitulo;
import com.example.spring_practica.repositorio.TipoTituloRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tipos-titulo")
public class TipoTituloControlador {

    private final TipoTituloRepositorio repositorio;

    public TipoTituloControlador(TipoTituloRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/tipos-titulo?page=0&size=20
    @GetMapping
    public Page<TipoTitulo> listar(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/tipos-titulo/5
    @GetMapping("/{id}")
    public ResponseEntity<TipoTitulo> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/tipos-titulo
    @PostMapping
    public ResponseEntity<TipoTitulo> crear(@RequestBody TipoTitulo nuevo) {
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/tipos-titulo/5
    @PutMapping("/{id}")
    public ResponseEntity<TipoTitulo> actualizar(@PathVariable Integer id, @RequestBody TipoTitulo datos) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        datos.setId(id);
        return ResponseEntity.ok(repositorio.save(datos));
    }

    // DELETE /api/tipos-titulo/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}