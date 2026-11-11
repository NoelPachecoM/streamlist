package com.example.spring_practica.controlador;

import com.example.spring_practica.config.SesionActual;
import com.example.spring_practica.servicio.MiListaServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * "Mi lista" que usa la pagina de Angular. Es aparte de /api/lista (el CRUD de la tabla).
 * Todas (menos /resenas) piden el token de sesion: el usuario sale del token, no de la peticion.
 *
 *   GET    /api/mi-lista
 *   POST   /api/mi-lista                 {"tmdbId":60735,"tipo":"serie","estado":"pendiente"}
 *   PUT    /api/mi-lista/5               {"estado":"completado","calificacion":9,"resena":"Muy buena"}
 *   DELETE /api/mi-lista/5
 *   GET    /api/mi-lista/resenas?tmdbId=60735&tipo=serie     (publica)
 *
 * Encabezado: Authorization: Bearer <token>
 * Estados validos: pendiente (por ver), viendo, completado.
 */
@RestController
@RequestMapping("/api/mi-lista")
public class MiListaControlador {

    private final MiListaServicio servicio;

    public MiListaControlador(MiListaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Map<String, Object>> listar(HttpServletRequest request) {
        return servicio.listar(SesionActual.usuario(request).getId());
    }

    @GetMapping("/resenas")
    public List<Map<String, Object>> resenas(@RequestParam Long tmdbId, @RequestParam String tipo) {
        return servicio.resenas(tmdbId, tipo);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> agregar(@RequestBody Map<String, Object> cuerpo,
                                                       HttpServletRequest request) {
        Integer usuarioId = SesionActual.usuario(request).getId();
        Object tmdbId = cuerpo.get("tmdbId");
        Object tipo = cuerpo.get("tipo");
        Object estado = cuerpo.get("estado");
        Map<String, Object> item = servicio.agregar(usuarioId, numero(tmdbId),
                tipo == null ? null : tipo.toString(),
                estado == null ? null : estado.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{id}")
    public Map<String, Object> actualizar(@PathVariable Integer id, @RequestBody Map<String, Object> cuerpo,
                                          HttpServletRequest request) {
        return servicio.actualizar(id, SesionActual.usuario(request).getId(), cuerpo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> quitar(@PathVariable Integer id, HttpServletRequest request) {
        servicio.quitar(id, SesionActual.usuario(request).getId());
        return ResponseEntity.noContent().build();
    }

    private Long numero(Object valor) {
        if (valor instanceof Number n) return n.longValue();
        if (valor != null) {
            try {
                return Long.valueOf(valor.toString().trim());
            } catch (NumberFormatException e) {
                // cae al null: el servicio responde "ID de TMDB invalido"
            }
        }
        return null;
    }
}
