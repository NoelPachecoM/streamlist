package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Plataforma;
import com.example.spring_practica.repositorio.PlataformaRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plataformas")
public class PlataformaControlador {

    private final PlataformaRepositorio repositorio;

    public PlataformaControlador(PlataformaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/plataformas?page=0&size=20
    @GetMapping
    public Page<Plataforma> listar(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/plataformas/5
    @GetMapping("/{id}")
    public ResponseEntity<Plataforma> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/plataformas
    @PostMapping
    public ResponseEntity<Plataforma> crear(@RequestBody Plataforma nuevo) {
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/plataformas/5
    @PutMapping("/{id}")
    public ResponseEntity<Plataforma> actualizar(@PathVariable Integer id, @RequestBody Plataforma datos) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        datos.setId(id);
        return ResponseEntity.ok(repositorio.save(datos));
    }

    // DELETE /api/plataformas/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}