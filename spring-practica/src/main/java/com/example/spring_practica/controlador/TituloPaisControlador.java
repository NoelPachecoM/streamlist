package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Pais;
import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.modelo.TituloPais;
import com.example.spring_practica.repositorio.PaisRepositorio;
import com.example.spring_practica.repositorio.TituloPaisRepositorio;
import com.example.spring_practica.repositorio.TituloRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/titulo-pais")
public class TituloPaisControlador {

    private final TituloPaisRepositorio repositorio;
    private final TituloRepositorio titulos;
    private final PaisRepositorio paises;

    public TituloPaisControlador(TituloPaisRepositorio repositorio, TituloRepositorio titulos,
                                 PaisRepositorio paises) {
        this.repositorio = repositorio;
        this.titulos = titulos;
        this.paises = paises;
    }

    // GET /api/titulo-pais?page=0&size=20&tituloId=3   (tituloId es opcional)
    @GetMapping
    public Page<TituloPais> listar(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) Integer tituloId) {
        PageRequest pagina = PageRequest.of(page, size);
        if (tituloId != null) {
            return repositorio.findByTitulo_Id(tituloId, pagina);
        }
        return repositorio.findAll(pagina);
    }

    // GET /api/titulo-pais/5
    @GetMapping("/{id}")
    public ResponseEntity<TituloPais> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/titulo-pais   {"titulo":{"id":1},"pais":{"id":1}}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TituloPais nuevo) {
        if (nuevo.getTitulo() == null || nuevo.getTitulo().getId() == null
                || nuevo.getPais() == null || nuevo.getPais().getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan titulo.id y/o pais.id"));
        }
        Titulo titulo = titulos.findById(nuevo.getTitulo().getId()).orElse(null);
        Pais pais = paises.findById(nuevo.getPais().getId()).orElse(null);
        if (titulo == null || pais == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El titulo o el pais no existe"));
        }
        if (repositorio.existsByTitulo_IdAndPais_Id(titulo.getId(), pais.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese titulo ya esta registrado en ese pais"));
        }
        nuevo.setId(null);
        nuevo.setTitulo(titulo);
        nuevo.setPais(pais);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/titulo-pais/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody TituloPais datos) {
        TituloPais actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        Titulo titulo = actual.getTitulo();
        Pais pais = actual.getPais();
        if (datos.getTitulo() != null && datos.getTitulo().getId() != null) {
            titulo = titulos.findById(datos.getTitulo().getId()).orElse(null);
        }
        if (datos.getPais() != null && datos.getPais().getId() != null) {
            pais = paises.findById(datos.getPais().getId()).orElse(null);
        }
        if (titulo == null || pais == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El titulo o el pais no existe"));
        }
        boolean repetido = repositorio.findByTitulo_IdAndPais_Id(titulo.getId(), pais.getId())
                .filter(otro -> !otro.getId().equals(id))
                .isPresent();
        if (repetido) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Ese titulo ya esta registrado en ese pais"));
        }
        actual.setTitulo(titulo);
        actual.setPais(pais);
        if (datos.getConsultadoEn() != null) actual.setConsultadoEn(datos.getConsultadoEn());
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/titulo-pais/5
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            repositorio.deleteById(id);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Tiene disponibilidades registradas. Borralas primero."));
        }
        return ResponseEntity.noContent().build();
    }
}
