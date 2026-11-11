package com.example.spring_practica.controlador;

import com.example.spring_practica.config.SesionActual;
import com.example.spring_practica.dto.ArcoRespuesta;
import com.example.spring_practica.dto.AuthPeticion;
import com.example.spring_practica.excepcion.ArcoException;
import com.example.spring_practica.modelo.RegistroPendiente;
import com.example.spring_practica.modelo.Rol;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.RegistroPendienteRepositorio;
import com.example.spring_practica.repositorio.RolRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import com.example.spring_practica.servicio.Contrasenas;
import com.example.spring_practica.servicio.ServicioCorreo;
import com.example.spring_practica.servicio.SesionServicio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro (con codigo de verificacion por correo) e inicio de sesion para el Angular.
 * La password se guarda cifrada con BCrypt (servicio Contrasenas).
 * El login regresa un token de sesion: el Angular lo manda en cada peticion como
 * "Authorization: Bearer <token>" (ver ProteccionApi).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthControlador {

    private static final int MINUTOS_VIGENCIA = 10;
    private static final int MAX_INTENTOS = 5;
    private static final int SEGUNDOS_ENTRE_ENVIOS = 60;

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final RegistroPendienteRepositorio pendientes;
    private final ServicioCorreo correo;
    private final Contrasenas contrasenas;
    private final SesionServicio sesiones;
    private final SecureRandom aleatorio = new SecureRandom();

    // Solo en modo pruebas (Postman): la respuesta incluye el codigo. El Angular nunca lo muestra.
    @Value("${arco.devolver-codigo:false}")
    private boolean devolverCodigo;

    public AuthControlador(UsuarioRepositorio usuarios,
                           RolRepositorio roles,
                           RegistroPendienteRepositorio pendientes,
                           ServicioCorreo correo,
                           Contrasenas contrasenas,
                           SesionServicio sesiones) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.pendientes = pendientes;
        this.correo = correo;
        this.contrasenas = contrasenas;
        this.sesiones = sesiones;
    }

    // ---------------- Registro en dos pasos ----------------

    // Paso 1: POST /api/auth/registro/solicitar
    //   {"nombre":"Ana","correo":"ana@correo.com","password":"Clave1234","telefono":"5512345678"}
    // No crea el usuario: guarda un registro pendiente y manda un codigo de 6 digitos al correo.
    @PostMapping("/registro/solicitar")
    public ArcoRespuesta solicitarRegistro(@RequestBody AuthPeticion p) {
        if (vacio(p.getNombre()) || vacio(p.getCorreo()) || vacio(p.getPassword())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Faltan nombre, correo y/o password");
        }
        String mail = p.getCorreo().trim();
        if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El correo no tiene un formato valido");
        }
        if (p.getNombre().trim().length() > 100 || mail.length() > 100) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El nombre y el correo no pueden pasar de 100 caracteres");
        }
        if (p.getTelefono() != null && p.getTelefono().length() > 20) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El telefono no puede pasar de 20 caracteres");
        }
        if (p.getPassword().length() < 8) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "La password debe tener al menos 8 caracteres");
        }
        if (p.getPassword().length() > 72) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "La password no puede pasar de 72 caracteres");
        }
        if (usuarios.existsByEmail(mail)) {
            throw new ArcoException(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
        }

        // Si ya habia un registro pendiente de este correo, se respeta la espera de 60 segundos
        // y despues el nuevo reemplaza al anterior.
        List<RegistroPendiente> previos = pendientes.findByCorreo(mail);
        for (RegistroPendiente previo : previos) {
            exigirEspera(previo);
        }
        pendientes.deleteAll(previos);

        RegistroPendiente nuevo = new RegistroPendiente();
        nuevo.setNombre(p.getNombre().trim());
        nuevo.setCorreo(mail);
        nuevo.setPassword(contrasenas.cifrar(p.getPassword())); // el registro pendiente ya guarda el hash
        nuevo.setTelefono(vacio(p.getTelefono()) ? null : p.getTelefono().trim());
        nuevo.setIntentos(0);
        asignarCodigo(nuevo);
        nuevo = pendientes.save(nuevo);

        enviarCodigo(nuevo);
        return respuesta(nuevo);
    }

    // Reenviar: POST /api/auth/registro/reenviar   {"solicitudId":7}
    @PostMapping("/registro/reenviar")
    public ArcoRespuesta reenviarRegistro(@RequestBody AuthPeticion p) {
        RegistroPendiente pendiente = buscarPendiente(p.getSolicitudId());
        exigirEspera(pendiente);
        asignarCodigo(pendiente);
        pendiente.setIntentos(0);
        pendiente = pendientes.save(pendiente);
        enviarCodigo(pendiente);
        return respuesta(pendiente);
    }

    // Paso 2: POST /api/auth/registro/confirmar   {"solicitudId":7,"codigo":"123456"}
    // Con el codigo correcto se crea el usuario (ROLE_CLIENTE) y se regresa el mismo resumen que el login.
    @PostMapping("/registro/confirmar")
    public ResponseEntity<?> confirmarRegistro(@RequestBody AuthPeticion p) {
        if (vacio(p.getCodigo())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Falta el codigo");
        }
        RegistroPendiente pendiente = buscarPendiente(p.getSolicitudId());

        if (pendiente.getIntentos() >= MAX_INTENTOS) {
            throw new ArcoException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Pide un codigo nuevo.");
        }
        if (pendiente.getExpiracion().isBefore(LocalDateTime.now())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El codigo vencio. Pide un codigo nuevo.");
        }
        if (!pendiente.getCodigo().equals(p.getCodigo().trim())) {
            pendiente.setIntentos(pendiente.getIntentos() + 1);
            pendientes.save(pendiente);
            int quedan = MAX_INTENTOS - pendiente.getIntentos();
            throw new ArcoException(HttpStatus.BAD_REQUEST, quedan > 0
                    ? "Codigo incorrecto. Te quedan " + quedan + " intento(s)."
                    : "Codigo incorrecto. Ya no te quedan intentos, pide un codigo nuevo.");
        }

        // Codigo correcto
        if (usuarios.existsByEmail(pendiente.getCorreo())) {
            pendientes.delete(pendiente);
            throw new ArcoException(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
        }
        Rol rolCliente = roles.findAll().stream()
                .filter(r -> "ROLE_CLIENTE".equals(r.getNombre()))
                .findFirst()
                .orElse(null);
        if (rolCliente == null) {
            throw new ArcoException(HttpStatus.INTERNAL_SERVER_ERROR, "No existe el rol ROLE_CLIENTE");
        }
        Usuario nuevo = new Usuario(null, pendiente.getNombre(), pendiente.getCorreo(), pendiente.getPassword(),
                pendiente.getTelefono(), true, LocalDateTime.now(), rolCliente);
        Usuario guardado = usuarios.save(nuevo);
        pendientes.delete(pendiente);
        return ResponseEntity.status(HttpStatus.CREATED).body(conToken(guardado));
    }

    // ---------------- Inicio de sesion ----------------

    // POST /api/auth/login   {"correo":"ana@correo.com","password":"Clave1234"}
    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@RequestBody AuthPeticion p) {
        if (vacio(p.getCorreo()) || vacio(p.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, "Faltan correo y/o password");
        }
        Usuario u = usuarios.findByEmail(p.getCorreo().trim()).orElse(null);
        if (u == null || !contrasenas.coincide(p.getPassword(), u.getPassword())) {
            return error(HttpStatus.UNAUTHORIZED, "Correo o password incorrectos");
        }
        if (Boolean.FALSE.equals(u.getActivo())) {
            return error(HttpStatus.FORBIDDEN, "La cuenta esta cancelada");
        }
        if (!contrasenas.esHash(u.getPassword())) {
            // cuenta vieja con password sin cifrar: se cifra en este momento
            u.setPassword(contrasenas.cifrar(p.getPassword()));
            u = usuarios.save(u);
        }
        return ResponseEntity.ok(conToken(u));
    }

    // GET /api/auth/sesion        (Authorization: Bearer <token>)
    // Regresa los datos actuales del usuario del token, leidos de la base (sobre todo el rol).
    // El Angular lo llama al abrir la pagina, asi un cambio de rol hecho en pgAdmin se nota al recargar.
    @GetMapping("/sesion")
    public Map<String, Object> sesion(HttpServletRequest request) {
        return resumen(SesionActual.usuario(request));
    }

    // POST /api/auth/logout       (Authorization: Bearer <token>)  borra la sesion de la base
    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request) {
        sesiones.cerrar(SesionActual.token(request));
        return ResponseEntity.noContent().build();
    }

    // ---------------- Auxiliares ----------------

    private RegistroPendiente buscarPendiente(Integer id) {
        if (id == null) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Falta solicitudId");
        }
        return pendientes.findById(id).orElseThrow(() -> new ArcoException(HttpStatus.NOT_FOUND,
                "No hay un registro pendiente. Empieza de nuevo."));
    }

    private void exigirEspera(RegistroPendiente pendiente) {
        long pasados = Duration.between(pendiente.getUltimoEnvio(), LocalDateTime.now()).getSeconds();
        if (pasados < SEGUNDOS_ENTRE_ENVIOS) {
            throw new ArcoException(HttpStatus.TOO_MANY_REQUESTS,
                    "Espera " + (SEGUNDOS_ENTRE_ENVIOS - pasados) + " segundos para pedir otro codigo.");
        }
    }

    private void asignarCodigo(RegistroPendiente pendiente) {
        LocalDateTime ahora = LocalDateTime.now();
        pendiente.setCodigo(String.format("%06d", aleatorio.nextInt(1_000_000)));
        pendiente.setExpiracion(ahora.plusMinutes(MINUTOS_VIGENCIA));
        pendiente.setUltimoEnvio(ahora);
    }

    private void enviarCodigo(RegistroPendiente pendiente) {
        try {
            correo.enviar(pendiente.getCorreo(),
                    "StreamList: tu codigo de verificacion",
                    "Hola " + pendiente.getNombre() + ",\n\n"
                            + "Tu codigo para crear tu cuenta en StreamList es: " + pendiente.getCodigo() + "\n\n"
                            + "Vale " + MINUTOS_VIGENCIA + " minutos. Si no fuiste tu, ignora este correo.");
        } catch (RuntimeException e) {
            // Si el correo no salio, no se deja un registro pendiente ni se activa la espera.
            pendientes.delete(pendiente);
            throw e;
        }
    }

    private ArcoRespuesta respuesta(RegistroPendiente pendiente) {
        return new ArcoRespuesta(
                "Te enviamos un codigo de 6 digitos a " + pendiente.getCorreo() + " (vale " + MINUTOS_VIGENCIA + " minutos)",
                pendiente.getId(),
                devolverCodigo ? pendiente.getCodigo() : null);
    }

    // Resumen + token de una sesion nueva (login y registro)
    private Map<String, Object> conToken(Usuario u) {
        Map<String, Object> datos = resumen(u);
        datos.put("token", sesiones.crear(u));
        return datos;
    }

    // Datos del usuario que se le regresan al Angular (nunca incluye la password)
    private Map<String, Object> resumen(Usuario u) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", u.getId());
        datos.put("nombre", u.getNombre());
        datos.put("correo", u.getEmail());
        datos.put("telefono", u.getTelefono());
        datos.put("rol", u.getRol() != null ? u.getRol().getNombre() : null);
        datos.put("suscripcion", Boolean.FALSE.equals(u.getActivo()) ? "cancelada" : "activa");
        return datos;
    }

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
