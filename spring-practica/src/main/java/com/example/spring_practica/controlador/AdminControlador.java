package com.example.spring_practica.controlador;

import com.example.spring_practica.config.SesionActual;
import com.example.spring_practica.servicio.AdminServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Panel de Administracion del Angular. El administrador sale del token de sesion
 * (Authorization: Bearer <token>) y Spring revisa en la base que sea ROLE_ADMIN.
 *
 *   GET  /api/admin/usuarios
 *   PUT  /api/admin/usuarios/5/rol                 {"rol":"ROLE_ADMIN"}   (o ROLE_CLIENTE)
 *   GET  /api/admin/resenas
 *   POST /api/admin/resenas/7/eliminar-texto
 *   GET  /api/admin/bitacora
 */
@RestController
@RequestMapping("/api/admin")
public class AdminControlador {

    private final AdminServicio servicio;

    public AdminControlador(AdminServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios")
    public List<Map<String, Object>> usuarios(HttpServletRequest request) {
        return servicio.usuarios(adminId(request));
    }

    @PutMapping("/usuarios/{id}/rol")
    public Map<String, Object> cambiarRol(@PathVariable Integer id, @RequestBody Map<String, Object> cuerpo,
                                          HttpServletRequest request) {
        Object rol = cuerpo.get("rol");
        return servicio.cambiarRol(adminId(request), id, rol == null ? null : rol.toString());
    }

    @GetMapping("/resenas")
    public List<Map<String, Object>> resenas(HttpServletRequest request) {
        return servicio.resenas(adminId(request));
    }

    @PostMapping("/resenas/{id}/eliminar-texto")
    public ResponseEntity<Void> eliminarTexto(@PathVariable Integer id, HttpServletRequest request) {
        servicio.eliminarTextoResena(adminId(request), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bitacora")
    public List<Map<String, Object>> bitacora(HttpServletRequest request) {
        return servicio.bitacora(adminId(request));
    }

    private Integer adminId(HttpServletRequest request) {
        return SesionActual.usuario(request).getId();
    }
}
