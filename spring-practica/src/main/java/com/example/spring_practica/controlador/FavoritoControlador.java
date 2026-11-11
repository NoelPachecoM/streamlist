package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Favorito;
import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.repositorio.FavoritoRepositorio;
import com.example.spring_practica.repositorio.TituloRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoControlador {

    private final FavoritoRepositorio repositorio;
    private final TituloRepositorio titulos;

    public FavoritoControlador(FavoritoRepositorio repositorio, TituloRepositorio titulos) {
        this.repositorio = repositorio;
        this.titulos = titulos;
    }

    // GET /api/favoritos?page=0&size=20&usuarioId=1   (usuarioId es opcional)
    @GetMapping
    public Page<Favorito> listar(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) Integer usuarioId) {
        PageRequest pagina = PageRequest.of(page, size);
        if (usuarioId != null) {
            return repositorio.findByUsuarioId(usuarioId, pagina);
        }
        return repositorio.findAll(pagina);
    }

    // GET /api/favoritos/5
    @GetMapping("/{id}")
    public ResponseEntity<Favorito> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/favoritos   {"usuarioId":1,"titulo":{"id":1}}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Favorito nuevo) {
        if (nuevo.getUsuarioId() == null || nuevo.getTitulo() == null || nuevo.getTitulo().getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan usuarioId y/o titulo.id"));
        }
        Titulo titulo = titulos.findById(nuevo.getTitulo().getId()).orElse(null);
        if (titulo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El titulo no existe"));
        }
        if (repositorio.existsByUsuarioIdAndTitulo_Id(nuevo.getUsuarioId(), titulo.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese titulo ya es favorito del usuario"));
        }
        nuevo.setId(null);
        nuevo.setTitulo(titulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/favoritos/5   (solo cambia el usuario o el titulo si se mandan)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Favorito datos) {
        Favorito actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        Integer usuarioId = datos.getUsuarioId() != null ? datos.getUsuarioId() : actual.getUsuarioId();
        Titulo titulo = actual.getTitulo();
        if (datos.getTitulo() != null && datos.getTitulo().getId() != null) {
            titulo = titulos.findById(datos.getTitulo().getId()).orElse(null);
            if (titulo == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El titulo no existe"));
            }
        }
        final Titulo tituloFinal = titulo;
        boolean repetido = repositorio.findByUsuarioIdAndTitulo_Id(usuarioId, tituloFinal.getId())
                .filter(otro -> !otro.getId().equals(id))
                .isPresent();
        if (repetido) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese titulo ya es favorito del usuario"));
        }
        actual.setUsuarioId(usuarioId);
        actual.setTitulo(tituloFinal);
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/favoritos/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
