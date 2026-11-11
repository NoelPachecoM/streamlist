package com.example.spring_practica.controlador;

import com.example.spring_practica.modelo.Rol;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.RolRepositorio;
import com.example.spring_practica.repositorio.SolicitudArcoRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import com.example.spring_practica.servicio.Contrasenas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioControlador {

    private final UsuarioRepositorio repositorio;
    private final RolRepositorio rolRepositorio;
    private final SolicitudArcoRepositorio solicitudRepositorio;
    private final Contrasenas contrasenas;

    public UsuarioControlador(UsuarioRepositorio repositorio, RolRepositorio rolRepositorio,
                              SolicitudArcoRepositorio solicitudRepositorio, Contrasenas contrasenas) {
        this.repositorio = repositorio;
        this.rolRepositorio = rolRepositorio;
        this.solicitudRepositorio = solicitudRepositorio;
        this.contrasenas = contrasenas;
    }

    // GET /api/usuarios?page=0&size=20
    @GetMapping
    public Page<Usuario> listar(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return repositorio.findAll(PageRequest.of(page, size));
    }

    // GET /api/usuarios/5
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtener(@PathVariable Integer id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/usuarios   (el rol se manda como "rol": {"id": 2})
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Usuario nuevo) {
        if (nuevo.getRol() == null || nuevo.getRol().getId() == null) {
            return ResponseEntity.badRequest().body("Falta rol.id");
        }
        Rol rol = rolRepositorio.findById(nuevo.getRol().getId()).orElse(null);
        if (rol == null) {
            return ResponseEntity.badRequest().body("El rol indicado no existe");
        }
        nuevo.setId(null);
        nuevo.setRol(rol);
        // La password se guarda cifrada con BCrypt
        if (nuevo.getPassword() != null) nuevo.setPassword(contrasenas.cifrarSiHaceFalta(nuevo.getPassword()));
        if (nuevo.getActivo() == null) nuevo.setActivo(true);
        if (nuevo.getFechaCreacion() == null) nuevo.setFechaCreacion(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(nuevo));
    }

    // PUT /api/usuarios/5   (solo cambia los campos que se manden)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Integer id, @RequestBody Usuario datos) {
        return repositorio.findById(id).map(actual -> {
            if (datos.getNombre() != null) actual.setNombre(datos.getNombre());
            if (datos.getEmail() != null) actual.setEmail(datos.getEmail());
            if (datos.getPassword() != null) actual.setPassword(contrasenas.cifrarSiHaceFalta(datos.getPassword()));
            if (datos.getTelefono() != null) actual.setTelefono(datos.getTelefono());
            if (datos.getActivo() != null) actual.setActivo(datos.getActivo());
            return ResponseEntity.ok(repositorio.save(actual));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/usuarios/5   (tambien elimina las solicitudes ARCO del usuario)
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        solicitudRepositorio.deleteByUsuarioId(id);
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
