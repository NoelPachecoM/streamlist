package com.example.spring_practica.controlador;

import com.example.spring_practica.dto.ArcoPeticion;
import com.example.spring_practica.dto.ArcoRespuesta;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import com.example.spring_practica.servicio.ArcoServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operaciones ARCO sobre los datos del usuario.
 * Todas son POST y reciben un JSON (ArcoPeticion); las respuestas son ArcoRespuesta
 * o {"error": "..."} cuando algo falla.
 */
@RestController
@RequestMapping("/api/arco")
public class ArcoControlador {

    private final ArcoServicio servicio;
    private final UsuarioRepositorio usuarioRepo;

    // Solo para la practica: si es true, GET /datos tambien muestra la password (desde Postman).
    // Ahora la password esta cifrada con BCrypt, asi que lo que se ve es su hash, no la password.
    @Value("${arco.devolver-codigo:false}")
    private boolean modoPruebas;

    public ArcoControlador(ArcoServicio servicio, UsuarioRepositorio usuarioRepo) {
        this.servicio = servicio;
        this.usuarioRepo = usuarioRepo;
    }

    // GET /api/arco/datos?correo=alguien@correo.com
    // Muestra los datos actuales del usuario y el estado de su suscripcion (activa / cancelada)
    @GetMapping("/datos")
    public ResponseEntity<?> verDatos(@RequestParam String correo) {
        Usuario u = usuarioRepo.findByEmail(correo).orElse(null);
        if (u == null) {
            return ResponseEntity.status(404).body(Map.of("error", "No existe un usuario con ese correo"));
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", u.getId());
        datos.put("nombre", u.getNombre());
        datos.put("correo", u.getEmail());
        datos.put("telefono", u.getTelefono());
        datos.put("password", modoPruebas ? u.getPassword() : "(oculta)");
        datos.put("suscripcion", Boolean.FALSE.equals(u.getActivo()) ? "cancelada" : "activa");
        return ResponseEntity.ok(datos);
    }

    // correo|nombre + password + passwordNueva
    @PostMapping("/cambiar-password")
    public ArcoRespuesta cambiarPassword(@RequestBody ArcoPeticion p) {
        return servicio.cambiarPassword(p);
    }

    // correo + password + nombreNuevo
    @PostMapping("/cambiar-nombre")
    public ArcoRespuesta cambiarNombre(@RequestBody ArcoPeticion p) {
        return servicio.cambiarNombre(p);
    }

    // nombre + password + correoNuevo
    @PostMapping("/cambiar-correo")
    public ArcoRespuesta cambiarCorreo(@RequestBody ArcoPeticion p) {
        return servicio.cambiarCorreo(p);
    }

    // Cambio de correo con doble verificacion (el Angular usa estos 3 pasos):
    // 1) correo actual + password + correoNuevo  -> manda un codigo al correo ACTUAL
    @PostMapping("/cambiar-correo/solicitar")
    public ArcoRespuesta solicitarCambioCorreo(@RequestBody ArcoPeticion p) {
        return servicio.solicitarCambioCorreo(p);
    }

    // 2) solicitudId + codigo (del correo actual) -> manda un codigo al correo NUEVO
    @PostMapping("/cambiar-correo/confirmar-actual")
    public ArcoRespuesta confirmarCorreoActual(@RequestBody ArcoPeticion p) {
        return servicio.confirmarCorreoActual(p);
    }

    // 3) solicitudId + codigo (del correo nuevo) -> cambia el correo
    @PostMapping("/cambiar-correo/confirmar-nuevo")
    public ArcoRespuesta confirmarCorreoNuevo(@RequestBody ArcoPeticion p) {
        return servicio.confirmarCorreoNuevo(p);
    }

    // correo|nombre + password + telefonoNuevo
    @PostMapping("/cambiar-telefono")
    public ArcoRespuesta cambiarTelefono(@RequestBody ArcoPeticion p) {
        return servicio.cambiarTelefono(p);
    }

    // Olvide mi password, paso 1: correo + nombre  -> manda un token al correo
    @PostMapping("/olvide-password/solicitar")
    public ArcoRespuesta solicitarRecuperacion(@RequestBody ArcoPeticion p) {
        return servicio.solicitarRecuperacion(p);
    }

    // Olvide mi password, paso 2: solicitudId + token + passwordNueva
    @PostMapping("/olvide-password/confirmar")
    public ArcoRespuesta confirmarRecuperacion(@RequestBody ArcoPeticion p) {
        return servicio.confirmarRecuperacion(p);
    }

    // Cancelar suscripcion, paso 1: correo|nombre + password -> manda un codigo al correo
    @PostMapping("/cancelar/solicitar")
    public ArcoRespuesta solicitarCancelacion(@RequestBody ArcoPeticion p) {
        return servicio.solicitarCancelacion(p);
    }

    // Cancelar suscripcion, paso 2: solicitudId + codigo
    @PostMapping("/cancelar/confirmar")
    public ArcoRespuesta confirmarCancelacion(@RequestBody ArcoPeticion p) {
        return servicio.confirmarCancelacion(p);
    }
}