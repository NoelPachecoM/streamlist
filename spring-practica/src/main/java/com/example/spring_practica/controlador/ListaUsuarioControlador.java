package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.ListaUsuario;
import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.repositorio.ListaUsuarioRepositorio;
import com.example.spring_practica.repositorio.TituloRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lista")
public class ListaUsuarioControlador {

    private final ListaUsuarioRepositorio repositorio;
    private final TituloRepositorio titulos;

    public ListaUsuarioControlador(ListaUsuarioRepositorio repositorio, TituloRepositorio titulos) {
        this.repositorio = repositorio;
        this.titulos = titulos;
    }

    // GET /api/lista?page=0&size=20&usuarioId=1   (usuarioId es opcional)
    @GetMapping
    public Page<ListaUsuario> listar(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) Integer usuarioId) {
        PageRequest pagina = PageRequest.of(page, size);
        if (usuarioId != null) {
            return repositorio.findByUsuarioId(usuarioId, pagina);
        }
        return repositorio.findAll(pagina);
    }

    // GET /api/lista/5
    @GetMapping("/{id}")
    public ResponseEntity<ListaUsuario> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/lista
    // {"usuarioId":1,"titulo":{"id":1},"estadoId":1,"calificacion":9,"resena":"Muy buena"}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ListaUsuario nuevo) {
        if (nuevo.getUsuarioId() == null || nuevo.getEstadoId() == null
                || nuevo.getTitulo() == null || nuevo.getTitulo().getId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Faltan usuarioId, estadoId y/o titulo.id"));
        }
        if (nuevo.getCalificacion() != null && (nuevo.getCalificacion() < 0 || nuevo.getCalificacion() > 10)) {
            return ResponseEntity.badRequest().body(Map.of("error", "La calificacion va de 0 a 10"));
        }
        Titulo titulo = titulos.findById(nuevo.getTitulo().getId()).orElse(null);
        if (titulo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El titulo no existe"));
        }
        if (repositorio.existsByUsuarioIdAndTitulo_Id(nuevo.getUsuarioId(), titulo.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese titulo ya esta en la lista del usuario"));
        }
        nuevo.setId(null);
        nuevo.setTitulo(titulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/lista/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody ListaUsuario datos) {
        ListaUsuario actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        if (datos.getCalificacion() != null && (datos.getCalificacion() < 0 || datos.getCalificacion() > 10)) {
            return ResponseEntity.badRequest().body(Map.of("error", "La calificacion va de 0 a 10"));
        }
        if (datos.getEstadoId() != null) actual.setEstadoId(datos.getEstadoId());
        if (datos.getCalificacion() != null) actual.setCalificacion(datos.getCalificacion());
        if (datos.getResena() != null) actual.setResena(datos.getResena());
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/lista/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
