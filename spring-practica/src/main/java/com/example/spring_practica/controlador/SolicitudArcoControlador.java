package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.SolicitudArco;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.SolicitudArcoRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/solicitudes-arco")
public class SolicitudArcoControlador {

    private static final Set<String> TIPOS =
            Set.of("ACCESO", "RECTIFICACION", "CANCELACION", "OPOSICION");
    private static final Set<String> ESTADOS =
            Set.of("PENDIENTE", "EN_PROCESO", "COMPLETADO", "RECHAZADO");

    private final SolicitudArcoRepositorio repositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    public SolicitudArcoControlador(SolicitudArcoRepositorio repositorio,
                                    UsuarioRepositorio usuarioRepositorio) {
        this.repositorio = repositorio;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    // GET /api/solicitudes-arco?page=0&size=20
    @GetMapping
    public Page<SolicitudArco> listar(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/solicitudes-arco/5
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudArco> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/solicitudes-arco
    // El estado inicial siempre es PENDIENTE y la fecha de solicitud es la actual.
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody SolicitudArco nueva) {
        if (nueva.getUsuario() == null || nueva.getUsuario().getId() == null) {
            return ResponseEntity.badRequest().body("Falta usuario.id");
        }
        if (nueva.getTipoDerecho() == null || !TIPOS.contains(nueva.getTipoDerecho())) {
            return ResponseEntity.badRequest()
                    .body("tipoDerecho debe ser ACCESO, RECTIFICACION, CANCELACION u OPOSICION");
        }
        if (nueva.getDetalles() == null || nueva.getDetalles().isBlank()) {
            return ResponseEntity.badRequest().body("Faltan los detalles de la solicitud");
        }
        Usuario usuario = usuarioRepositorio.findById(nueva.getUsuario().getId()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.badRequest().body("El usuario indicado no existe");
        }
        nueva.setId(null);
        nueva.setUsuario(usuario);
        nueva.setEstado("PENDIENTE");
        nueva.setFechaSolicitud(LocalDateTime.now());
        nueva.setFechaRespuesta(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nueva));
    }

    // PUT /api/solicitudes-arco/5
    // Si el estado pasa a COMPLETADO o RECHAZADO se registra la fecha de respuesta.
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody SolicitudArco datos) {
        SolicitudArco actual = repositorio.findById(id).orElse(null);
        if (actual == null) {
            return ResponseEntity.notFound().build();
        }
        if (datos.getTipoDerecho() != null) {
            if (!TIPOS.contains(datos.getTipoDerecho())) {
                return ResponseEntity.badRequest().body("tipoDerecho no valido");
            }
            actual.setTipoDerecho(datos.getTipoDerecho());
        }
        if (datos.getDetalles() != null) {
            actual.setDetalles(datos.getDetalles());
        }
        if (datos.getEstado() != null) {
            if (!ESTADOS.contains(datos.getEstado())) {
                return ResponseEntity.badRequest().body("estado no valido");
            }
            actual.setEstado(datos.getEstado());
            if (datos.getEstado().equals("COMPLETADO") || datos.getEstado().equals("RECHAZADO")) {
                actual.setFechaRespuesta(LocalDateTime.now());
            }
        }
        return ResponseEntity.ok(repositorio.save(actual));
    }

    // DELETE /api/solicitudes-arco/5
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}