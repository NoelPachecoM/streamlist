package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Actor;
import com.example.spring_practica.modelo.Reparto;
import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.repositorio.ActorRepositorio;
import com.example.spring_practica.repositorio.RepartoRepositorio;
import com.example.spring_practica.repositorio.TituloRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reparto")
public class RepartoControlador {

    private final RepartoRepositorio repositorio;
    private final TituloRepositorio titulos;
    private final ActorRepositorio actores;

    public RepartoControlador(RepartoRepositorio repositorio, TituloRepositorio titulos,
                              ActorRepositorio actores) {
        this.repositorio = repositorio;
        this.titulos = titulos;
        this.actores = actores;
    }

    // GET /api/reparto?page=0&size=20&tituloId=3   (tituloId es opcional)
    @GetMapping
    public Page<Reparto> listar(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) Integer tituloId) {
        PageRequest pagina = PageRequest.of(page, size);
        if (tituloId != null) {
            return repositorio.findByTitulo_Id(tituloId, pagina);
        }
        return repositorio.findAll(pagina);
    }

    // GET /api/reparto/5
    @GetMapping("/{id}")
    public ResponseEntity<Reparto> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/reparto   {"titulo":{"id":1},"actor":{"idActor":1},"personaId":null,"orden":1,"personaje":"Cobb"}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Reparto nuevo) {
        if (nuevo.getTitulo() == null || nuevo.getTitulo().getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta titulo.id"));
        }
        boolean traeActor = nuevo.getActor() != null && nuevo.getActor().getIdActor() != null;
        if (!traeActor && nuevo.getPersonaId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Manda actor.idActor y/o personaId"));
        }
        Titulo titulo = titulos.findById(nuevo.getTitulo().getId()).orElse(null);
        if (titulo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El titulo no existe"));
        }
        Actor actor = null;
        if (traeActor) {
            actor = actores.findById(nuevo.getActor().getIdActor()).orElse(null);
            if (actor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El actor no existe"));
            }
        }
        nuevo.setId(null);
        nuevo.setTitulo(titulo);
        nuevo.setActor(actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/reparto/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Reparto datos) {
        Reparto actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        if (datos.getTitulo() != null && datos.getTitulo().getId() != null) {
            Titulo titulo = titulos.findById(datos.getTitulo().getId()).orElse(null);
            if (titulo == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El titulo no existe"));
            }
            actual.setTitulo(titulo);
        }
        if (datos.getActor() != null && datos.getActor().getIdActor() != null) {
            Actor actor = actores.findById(datos.getActor().getIdActor()).orElse(null);
            if (actor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El actor no existe"));
            }
            actual.setActor(actor);
        }
        if (datos.getPersonaId() != null) actual.setPersonaId(datos.getPersonaId());
        if (datos.getOrden() != null) actual.setOrden(datos.getOrden());
        if (datos.getPersonaje() != null) actual.setPersonaje(datos.getPersonaje());
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/reparto/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
