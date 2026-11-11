package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Pais;
import com.example.spring_practica.repositorio.PaisRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/paises")
public class PaisControlador {

    private final PaisRepositorio repositorio;

    public PaisControlador(PaisRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/paises?page=0&size=20
    @GetMapping
    public Page<Pais> listar(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/paises/5
    @GetMapping("/{id}")
    public ResponseEntity<Pais> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/paises   {"codigo":"PE","nombre":"Peru","idioma":"es-PE","region":"Latinoamerica"}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Pais nuevo) {
        if (nuevo.getCodigo() == null || nuevo.getCodigo().isBlank()
                || nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan codigo y/o nombre"));
        }
        nuevo.setCodigo(nuevo.getCodigo().trim().toUpperCase());
        if (repositorio.existsByCodigo(nuevo.getCodigo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un pais con el codigo " + nuevo.getCodigo()));
        }
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/paises/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Pais datos) {
        Pais actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        if (datos.getCodigo() != null && !datos.getCodigo().isBlank()) {
            String codigo = datos.getCodigo().trim().toUpperCase();
            boolean usadoPorOtro = repositorio.findByCodigo(codigo)
                    .filter(otro -> !otro.getId().equals(id))
                    .isPresent();
            if (usadoPorOtro) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Ya existe otro pais con el codigo " + codigo));
            }
            actual.setCodigo(codigo);
        }
        if (datos.getNombre() != null && !datos.getNombre().isBlank()) actual.setNombre(datos.getNombre());
        if (datos.getIdioma() != null) actual.setIdioma(datos.getIdioma());
        if (datos.getRegion() != null) actual.setRegion(datos.getRegion());
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/paises/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
