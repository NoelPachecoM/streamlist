package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.TipoDisponibilidad;
import com.example.spring_practica.repositorio.TipoDisponibilidadRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tipos-disponibilidad")
public class TipoDisponibilidadControlador {

    private final TipoDisponibilidadRepositorio repositorio;

    public TipoDisponibilidadControlador(TipoDisponibilidadRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    // GET /api/tipos-disponibilidad?page=0&size=20
    @GetMapping
    public Page<TipoDisponibilidad> listar(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/tipos-disponibilidad/5
    @GetMapping("/{id}")
    public ResponseEntity<TipoDisponibilidad> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/tipos-disponibilidad   {"clave":"gratis","etiqueta":"Gratis"}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TipoDisponibilidad nuevo) {
        if (nuevo.getClave() == null || nuevo.getClave().isBlank()
                || nuevo.getEtiqueta() == null || nuevo.getEtiqueta().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan clave y/o etiqueta"));
        }
        nuevo.setClave(nuevo.getClave().trim().toLowerCase());
        if (repositorio.existsByClave(nuevo.getClave())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ya existe un tipo con la clave " + nuevo.getClave()));
        }
        nuevo.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/tipos-disponibilidad/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody TipoDisponibilidad datos) {
        TipoDisponibilidad actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        if (datos.getClave() != null && !datos.getClave().isBlank()) {
            String clave = datos.getClave().trim().toLowerCase();
            boolean usadaPorOtro = repositorio.findByClave(clave)
                    .filter(otro -> !otro.getId().equals(id))
                    .isPresent();
            if (usadaPorOtro) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Ya existe otro tipo con la clave " + clave));
            }
            actual.setClave(clave);
        }
        if (datos.getEtiqueta() != null && !datos.getEtiqueta().isBlank()) {
            actual.setEtiqueta(datos.getEtiqueta());
        }
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/tipos-disponibilidad/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
