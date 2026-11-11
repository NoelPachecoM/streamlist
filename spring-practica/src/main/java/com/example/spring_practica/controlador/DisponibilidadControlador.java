package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Disponibilidad;
import com.example.spring_practica.modelo.TipoDisponibilidad;
import com.example.spring_practica.modelo.TituloPais;
import com.example.spring_practica.repositorio.DisponibilidadRepositorio;
import com.example.spring_practica.repositorio.TipoDisponibilidadRepositorio;
import com.example.spring_practica.repositorio.TituloPaisRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadControlador {

    private final DisponibilidadRepositorio repositorio;
    private final TituloPaisRepositorio tituloPaises;
    private final TipoDisponibilidadRepositorio tipos;

    public DisponibilidadControlador(DisponibilidadRepositorio repositorio,
                                     TituloPaisRepositorio tituloPaises,
                                     TipoDisponibilidadRepositorio tipos) {
        this.repositorio = repositorio;
        this.tituloPaises = tituloPaises;
        this.tipos = tipos;
    }

    // GET /api/disponibilidad?page=0&size=20&tituloPaisId=3   (tituloPaisId es opcional)
    @GetMapping
    public Page<Disponibilidad> listar(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) Integer tituloPaisId) {
        PageRequest pagina = PageRequest.of(page, size);
        if (tituloPaisId != null) {
            return repositorio.findByTituloPais_Id(tituloPaisId, pagina);
        }
        return repositorio.findAll(pagina);
    }

    // GET /api/disponibilidad/5
    @GetMapping("/{id}")
    public ResponseEntity<Disponibilidad> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/disponibilidad   {"tituloPais":{"id":1},"plataformaId":1,"tipoDisponibilidad":{"id":1}}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Disponibilidad nuevo) {
        if (nuevo.getTituloPais() == null || nuevo.getTituloPais().getId() == null
                || nuevo.getPlataformaId() == null
                || nuevo.getTipoDisponibilidad() == null || nuevo.getTipoDisponibilidad().getId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Faltan tituloPais.id, plataformaId y/o tipoDisponibilidad.id"));
        }
        TituloPais tp = tituloPaises.findById(nuevo.getTituloPais().getId()).orElse(null);
        TipoDisponibilidad tipo = tipos.findById(nuevo.getTipoDisponibilidad().getId()).orElse(null);
        if (tp == null || tipo == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El tituloPais o el tipoDisponibilidad no existe"));
        }
        if (repositorio.existsByTituloPais_IdAndPlataformaIdAndTipoDisponibilidad_Id(
                tp.getId(), nuevo.getPlataformaId(), tipo.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Esa disponibilidad ya existe"));
        }
        nuevo.setId(null);
        nuevo.setTituloPais(tp);
        nuevo.setTipoDisponibilidad(tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/disponibilidad/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Disponibilidad datos) {
        Disponibilidad actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        TituloPais tp = actual.getTituloPais();
        TipoDisponibilidad tipo = actual.getTipoDisponibilidad();
        if (datos.getTituloPais() != null && datos.getTituloPais().getId() != null) {
            tp = tituloPaises.findById(datos.getTituloPais().getId()).orElse(null);
        }
        if (datos.getTipoDisponibilidad() != null && datos.getTipoDisponibilidad().getId() != null) {
            tipo = tipos.findById(datos.getTipoDisponibilidad().getId()).orElse(null);
        }
        if (tp == null || tipo == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El tituloPais o el tipoDisponibilidad no existe"));
        }
        Integer plataformaId = datos.getPlataformaId() != null ? datos.getPlataformaId() : actual.getPlataformaId();
        boolean repetido = repositorio
                .findByTituloPais_IdAndPlataformaIdAndTipoDisponibilidad_Id(tp.getId(), plataformaId, tipo.getId())
                .filter(otro -> !otro.getId().equals(id))
                .isPresent();
        if (repetido) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Esa disponibilidad ya existe"));
        }
        actual.setTituloPais(tp);
        actual.setTipoDisponibilidad(tipo);
        actual.setPlataformaId(plataformaId);
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/disponibilidad/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
