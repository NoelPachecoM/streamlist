package com.example.spring_practica.servicio;

import com.example.spring_practica.dto.ArcoPeticion;
import com.example.spring_practica.dto.ArcoRespuesta;
import com.example.spring_practica.excepcion.ArcoException;
import com.example.spring_practica.modelo.CodigoVerificacion;
import com.example.spring_practica.modelo.SolicitudArco;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.CodigoVerificacionRepositorio;
import com.example.spring_practica.repositorio.SolicitudArcoRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Logica de los derechos ARCO sobre los datos del usuario
 * (correo, nombre, password y telefono).
 *
 * Cada operacion exitosa queda registrada en la tabla solicitudes_arco.
 */
@Service
public class ArcoServicio {

    private static final String RECTIFICACION = "RECTIFICACION";
    private static final String CANCELACION = "CANCELACION";

    private static final String TIPO_TOKEN = "RECUPERAR_PASSWORD";
    private static final String TIPO_CODIGO = "CANCELAR_SUSCRIPCION";

    private static final String TIPO_CORREO_ACTUAL = "CAMBIAR_CORREO_ACTUAL";
    private static final String TIPO_CORREO_NUEVO = "CAMBIAR_CORREO_NUEVO";

    private static final int MINUTOS_TOKEN = 15;
    private static final int MINUTOS_CODIGO = 10;
    private static final int MINUTOS_CORREO = 10;
    private static final int MAX_INTENTOS = 5;

    private final UsuarioRepositorio usuarioRepo;
    private final SolicitudArcoRepositorio solicitudRepo;
    private final CodigoVerificacionRepositorio codigoRepo;
    private final ServicioCorreo servicioCorreo;
    private final Contrasenas contrasenas;
    private final SesionServicio sesiones;
    private final SecureRandom aleatorio = new SecureRandom();

    // Si es true, la respuesta incluye el token/codigo (solo para pruebas con Postman)
    @Value("${arco.devolver-codigo:false}")
    private boolean devolverCodigo;

    public ArcoServicio(UsuarioRepositorio usuarioRepo,
                        SolicitudArcoRepositorio solicitudRepo,
                        CodigoVerificacionRepositorio codigoRepo,
                        ServicioCorreo servicioCorreo,
                        Contrasenas contrasenas,
                        SesionServicio sesiones) {
        this.usuarioRepo = usuarioRepo;
        this.solicitudRepo = solicitudRepo;
        this.codigoRepo = codigoRepo;
        this.servicioCorreo = servicioCorreo;
        this.contrasenas = contrasenas;
        this.sesiones = sesiones;
    }

    // =====================================================================
    // 1. Cambiar password: se verifica con (correo y password) o (nombre y password)
    // =====================================================================
    @Transactional
    public ArcoRespuesta cambiarPassword(ArcoPeticion p) {
        Usuario usuario = verificarCorreoONombre(p);
        exigir(p.getPasswordNueva(), "passwordNueva");
        if (contrasenas.coincide(p.getPasswordNueva(), usuario.getPassword())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST,
                    "La password nueva debe ser diferente a la actual");
        }
        usuario.setPassword(contrasenas.cifrar(p.getPasswordNueva()));
        usuarioRepo.save(usuario);
        SolicitudArco solicitud = registrar(usuario, RECTIFICACION, "Cambio de password", "COMPLETADO");
        return new ArcoRespuesta("Password actualizada", solicitud.getId(), null);
    }

    // =====================================================================
    // 2. Cambiar nombre: se verifica con (correo y password)
    // =====================================================================
    @Transactional
    public ArcoRespuesta cambiarNombre(ArcoPeticion p) {
        Usuario usuario = verificarCorreoYPassword(p.getCorreo(), p.getPassword());
        exigir(p.getNombreNuevo(), "nombreNuevo");
        limitar(p.getNombreNuevo(), 100, "nombreNuevo");
        String anterior = usuario.getNombre();
        usuario.setNombre(p.getNombreNuevo());
        usuarioRepo.save(usuario);
        SolicitudArco solicitud = registrar(usuario, RECTIFICACION,
                "Cambio de nombre: de '" + anterior + "' a '" + p.getNombreNuevo() + "'", "COMPLETADO");
        return new ArcoRespuesta("Nombre actualizado", solicitud.getId(), null);
    }

    // =====================================================================
    // 3. Cambiar correo: se verifica con (nombre y password)
    // =====================================================================
    @Transactional
    public ArcoRespuesta cambiarCorreo(ArcoPeticion p) {
        Usuario usuario = verificarNombreYPassword(p.getNombre(), p.getPassword());
        exigir(p.getCorreoNuevo(), "correoNuevo");
        limitar(p.getCorreoNuevo(), 100, "correoNuevo");
        if (!p.getCorreoNuevo().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El correo nuevo no tiene un formato valido");
        }
        if (p.getCorreoNuevo().equals(usuario.getEmail())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El correo nuevo debe ser diferente al actual");
        }
        if (usuarioRepo.existsByEmail(p.getCorreoNuevo())) {
            throw new ArcoException(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
        }
        String anterior = usuario.getEmail();
        usuario.setEmail(p.getCorreoNuevo());
        usuarioRepo.save(usuario);
        SolicitudArco solicitud = registrar(usuario, RECTIFICACION,
                "Cambio de correo: de '" + anterior + "' a '" + p.getCorreoNuevo() + "'", "COMPLETADO");
        return new ArcoRespuesta("Correo actualizado", solicitud.getId(), null);
    }

    // =====================================================================
    // 3b. Cambiar correo con DOBLE verificacion (3 pasos)
    //     1) correo actual + password + correoNuevo -> codigo al correo ACTUAL
    //     2) codigo del correo actual               -> codigo al correo NUEVO
    //     3) codigo del correo nuevo                -> se cambia el correo
    //     La ruta de un solo paso (cambiar-correo) se conserva para la coleccion de Postman.
    // =====================================================================
    @Transactional
    public ArcoRespuesta solicitarCambioCorreo(ArcoPeticion p) {
        Usuario usuario = verificarCorreoYPassword(p.getCorreo(), p.getPassword());
        exigir(p.getCorreoNuevo(), "correoNuevo");
        String nuevo = p.getCorreoNuevo().trim();
        limitar(nuevo, 100, "correoNuevo");
        if (!nuevo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El correo nuevo no tiene un formato valido");
        }
        if (nuevo.equalsIgnoreCase(usuario.getEmail())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El correo nuevo debe ser diferente al actual");
        }
        if (usuarioRepo.existsByEmail(nuevo)) {
            throw new ArcoException(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
        }

        SolicitudArco solicitud = registrar(usuario, RECTIFICACION,
                "Cambio de correo: se envio un codigo al correo actual para confirmar", "EN_PROCESO");
        String codigo = generarCodigo();
        guardarCodigo(solicitud, codigo, TIPO_CORREO_ACTUAL, MINUTOS_CORREO, nuevo);

        servicioCorreo.enviar(usuario.getEmail(), "StreamList - Confirma el cambio de tu correo",
                "Hola " + usuario.getNombre() + ",\n\n"
                + "Recibimos una solicitud para cambiar el correo de tu cuenta.\n"
                + "Para confirmar que fuiste tu, ingresa este codigo: " + codigo + "\n"
                + "Tiene una vigencia de " + MINUTOS_CORREO + " minutos.\n\n"
                + "Si no fuiste tu, ignora este mensaje y cambia tu password.");

        return new ArcoRespuesta("Te enviamos un codigo a tu correo actual para confirmar el cambio",
                solicitud.getId(), devolverCodigo ? codigo : null);
    }

    // noRollbackFor: si el codigo es incorrecto, el intento fallido debe quedar guardado
    @Transactional(noRollbackFor = ArcoException.class)
    public ArcoRespuesta confirmarCorreoActual(ArcoPeticion p) {
        exigir(p.getSolicitudId(), "solicitudId");
        exigir(p.getCodigo(), "codigo");

        CodigoVerificacion actual = validarCodigoConIntentos(p.getSolicitudId(), TIPO_CORREO_ACTUAL, p.getCodigo());
        SolicitudArco solicitud = actual.getSolicitud();
        Usuario usuario = solicitud.getUsuario();
        comprobarActivo(usuario);
        String nuevo = actual.getDatoNuevo();
        if (usuarioRepo.existsByEmail(nuevo)) {
            throw new ArcoException(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
        }

        String codigoNuevo = generarCodigo();
        guardarCodigo(solicitud, codigoNuevo, TIPO_CORREO_NUEVO, MINUTOS_CORREO, nuevo);
        servicioCorreo.enviar(nuevo, "StreamList - Confirma tu correo nuevo",
                "Hola " + usuario.getNombre() + ",\n\n"
                + "Pediste usar este correo en tu cuenta de StreamList.\n"
                + "Para confirmar que es tuyo, ingresa este codigo: " + codigoNuevo + "\n"
                + "Tiene una vigencia de " + MINUTOS_CORREO + " minutos.\n\n"
                + "Si no fuiste tu, ignora este mensaje.");

        // El codigo del correo actual se marca usado hasta que el correo nuevo ya salio
        actual.setUsado(true);
        codigoRepo.save(actual);
        solicitud.setDetalles("Cambio de correo: confirmado desde el correo actual, falta confirmar el correo nuevo");
        solicitudRepo.save(solicitud);

        return new ArcoRespuesta("Listo. Ahora te enviamos un codigo al correo nuevo para confirmar que existe",
                solicitud.getId(), devolverCodigo ? codigoNuevo : null);
    }

    @Transactional(noRollbackFor = ArcoException.class)
    public ArcoRespuesta confirmarCorreoNuevo(ArcoPeticion p) {
        exigir(p.getSolicitudId(), "solicitudId");
        exigir(p.getCodigo(), "codigo");

        CodigoVerificacion codigo = validarCodigoConIntentos(p.getSolicitudId(), TIPO_CORREO_NUEVO, p.getCodigo());
        SolicitudArco solicitud = codigo.getSolicitud();
        Usuario usuario = solicitud.getUsuario();
        comprobarActivo(usuario);
        String anterior = usuario.getEmail();
        String nuevo = codigo.getDatoNuevo();
        if (usuarioRepo.existsByEmail(nuevo)) {
            throw new ArcoException(HttpStatus.CONFLICT, "Ese correo ya esta registrado");
        }

        usuario.setEmail(nuevo);
        usuarioRepo.save(usuario);
        codigo.setUsado(true);
        codigoRepo.save(codigo);
        solicitud.setDetalles("Cambio de correo: de '" + anterior + "' a '" + nuevo
                + "' (confirmado en ambos correos)");
        cerrarSolicitud(solicitud, "COMPLETADO");
        return new ArcoRespuesta("Correo actualizado", solicitud.getId(), null);
    }

    // =====================================================================
    // 4. Cambiar telefono: se verifica con (nombre y password) o (correo y password)
    // =====================================================================
    @Transactional
    public ArcoRespuesta cambiarTelefono(ArcoPeticion p) {
        Usuario usuario = verificarCorreoONombre(p);
        exigir(p.getTelefonoNuevo(), "telefonoNuevo");
        limitar(p.getTelefonoNuevo(), 20, "telefonoNuevo");
        usuario.setTelefono(p.getTelefonoNuevo());
        usuarioRepo.save(usuario);
        SolicitudArco solicitud = registrar(usuario, RECTIFICACION, "Cambio de telefono", "COMPLETADO");
        return new ArcoRespuesta("Telefono actualizado", solicitud.getId(), null);
    }

    // =====================================================================
    // 5. Olvide mi password
    //    Paso 1: se verifica con (correo y nombre) y se manda un TOKEN al correo
    // =====================================================================
    @Transactional
    public ArcoRespuesta solicitarRecuperacion(ArcoPeticion p) {
        exigir(p.getCorreo(), "correo");
        exigir(p.getNombre(), "nombre");
        Usuario usuario = usuarioRepo.findByEmail(p.getCorreo()).orElse(null);
        if (usuario == null || !usuario.getNombre().equals(p.getNombre())) {
            throw credencialesIncorrectas();
        }
        comprobarActivo(usuario);

        SolicitudArco solicitud = registrar(usuario, RECTIFICACION,
                "Olvido de password: se envio un token de confirmacion al correo", "EN_PROCESO");
        String token = String.format("%06d", aleatorio.nextInt(1000000));
        guardarCodigo(solicitud, token, TIPO_TOKEN, MINUTOS_TOKEN);

        servicioCorreo.enviar(usuario.getEmail(), "StreamList - Recuperacion de password",
                "Hola " + usuario.getNombre() + ",\n\n"
                + "Recibimos una solicitud para restablecer tu password.\n"
                + "Tu token de confirmacion (6 digitos) es: " + token + "\n"
                + "Tiene una vigencia de " + MINUTOS_TOKEN + " minutos.\n\n"
                + "Si no fuiste tu, ignora este mensaje.");

        return new ArcoRespuesta("Se envio un token de confirmacion al correo registrado",
                solicitud.getId(), devolverCodigo ? token : null);
    }

    //    Paso 2: si el token coincide, se cambia la password
    @Transactional
    public ArcoRespuesta confirmarRecuperacion(ArcoPeticion p) {
        exigir(p.getSolicitudId(), "solicitudId");
        exigir(p.getToken(), "token");
        exigir(p.getPasswordNueva(), "passwordNueva");

        CodigoVerificacion codigo = validarCodigo(p.getSolicitudId(), TIPO_TOKEN, p.getToken());
        SolicitudArco solicitud = codigo.getSolicitud();
        Usuario usuario = solicitud.getUsuario();

        usuario.setPassword(contrasenas.cifrar(p.getPasswordNueva()));
        usuarioRepo.save(usuario);
        sesiones.cerrarTodas(usuario.getId()); // por seguridad se cierran sus sesiones abiertas
        codigo.setUsado(true);
        codigoRepo.save(codigo);
        cerrarSolicitud(solicitud, "COMPLETADO");
        return new ArcoRespuesta("Password restablecida", solicitud.getId(), null);
    }

    // =====================================================================
    // 6. Cancelar suscripcion
    //    Paso 1: se verifica con (correo o nombre) y password, y se manda un CODIGO al correo
    // =====================================================================
    @Transactional
    public ArcoRespuesta solicitarCancelacion(ArcoPeticion p) {
        Usuario usuario = verificarCorreoONombre(p);

        SolicitudArco solicitud = registrar(usuario, CANCELACION,
                "Cancelacion de suscripcion: se envio un codigo de confirmacion al correo", "EN_PROCESO");
        String codigo = String.format("%06d", aleatorio.nextInt(1000000));
        guardarCodigo(solicitud, codigo, TIPO_CODIGO, MINUTOS_CODIGO);

        servicioCorreo.enviar(usuario.getEmail(), "StreamList - Confirmacion para cancelar tu suscripcion",
                "Hola " + usuario.getNombre() + ",\n\n"
                + "Para cancelar tu suscripcion, ingresa este codigo: " + codigo + "\n"
                + "Tiene una vigencia de " + MINUTOS_CODIGO + " minutos.\n\n"
                + "Si no fuiste tu, ignora este mensaje y cambia tu password.");

        return new ArcoRespuesta("Se envio un codigo de confirmacion al correo registrado",
                solicitud.getId(), devolverCodigo ? codigo : null);
    }

    //    Paso 2: si el codigo coincide, se cancela la suscripcion (el usuario queda inactivo)
    @Transactional
    public ArcoRespuesta confirmarCancelacion(ArcoPeticion p) {
        exigir(p.getSolicitudId(), "solicitudId");
        exigir(p.getCodigo(), "codigo");

        CodigoVerificacion codigo = validarCodigo(p.getSolicitudId(), TIPO_CODIGO, p.getCodigo());
        SolicitudArco solicitud = codigo.getSolicitud();
        Usuario usuario = solicitud.getUsuario();

        usuario.setActivo(false);
        usuarioRepo.save(usuario);
        codigo.setUsado(true);
        codigoRepo.save(codigo);
        cerrarSolicitud(solicitud, "COMPLETADO");
        return new ArcoRespuesta("Suscripcion cancelada", solicitud.getId(), null);
    }

    // =====================================================================
    // Verificaciones de identidad
    // =====================================================================

    // Si viene correo se verifica con (correo y password); si no, con (nombre y password)
    private Usuario verificarCorreoONombre(ArcoPeticion p) {
        if (p.getCorreo() != null && !p.getCorreo().isBlank()) {
            return verificarCorreoYPassword(p.getCorreo(), p.getPassword());
        }
        return verificarNombreYPassword(p.getNombre(), p.getPassword());
    }

    private Usuario verificarCorreoYPassword(String correo, String password) {
        exigir(correo, "correo");
        exigir(password, "password");
        Usuario usuario = usuarioRepo.findByEmail(correo).orElse(null);
        if (usuario == null || !contrasenas.coincide(password, usuario.getPassword())) {
            throw credencialesIncorrectas();
        }
        comprobarActivo(usuario);
        return usuario;
    }

    private Usuario verificarNombreYPassword(String nombre, String password) {
        exigir(nombre, "nombre");
        exigir(password, "password");
        // Las passwords estan cifradas: se buscan los usuarios con ese nombre y se compara cada hash
        List<Usuario> coincidencias = usuarioRepo.findByNombre(nombre).stream()
                .filter(u -> contrasenas.coincide(password, u.getPassword()))
                .toList();
        // El nombre no es unico: si no hay exactamente una coincidencia, no se puede verificar
        if (coincidencias.size() != 1) {
            throw credencialesIncorrectas();
        }
        Usuario usuario = coincidencias.get(0);
        comprobarActivo(usuario);
        return usuario;
    }

    private void comprobarActivo(Usuario usuario) {
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new ArcoException(HttpStatus.FORBIDDEN, "La cuenta esta cancelada");
        }
    }

    private ArcoException credencialesIncorrectas() {
        return new ArcoException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    // =====================================================================
    // Apoyo
    // =====================================================================

    private CodigoVerificacion validarCodigo(Integer solicitudId, String tipo, String valor) {
        CodigoVerificacion codigo = codigoRepo
                .findFirstBySolicitudIdAndTipoAndUsadoFalseOrderByIdDesc(solicitudId, tipo)
                .orElse(null);
        if (codigo == null) {
            throw new ArcoException(HttpStatus.BAD_REQUEST,
                    "No hay una verificacion pendiente para esa solicitud");
        }
        if (codigo.getExpiracion().isBefore(LocalDateTime.now())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El codigo expiro, solicita uno nuevo");
        }
        if (!codigo.getCodigo().equals(valor)) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El codigo no coincide");
        }
        return codigo;
    }

    private void guardarCodigo(SolicitudArco solicitud, String valor, String tipo, int minutos) {
        guardarCodigo(solicitud, valor, tipo, minutos, null);
    }

    private void guardarCodigo(SolicitudArco solicitud, String valor, String tipo, int minutos, String datoNuevo) {
        CodigoVerificacion codigo = new CodigoVerificacion();
        codigo.setSolicitud(solicitud);
        codigo.setCodigo(valor);
        codigo.setTipo(tipo);
        codigo.setExpiracion(LocalDateTime.now().plusMinutes(minutos));
        codigo.setUsado(false);
        codigo.setDatoNuevo(datoNuevo);
        codigo.setIntentos(0);
        codigoRepo.save(codigo);
    }

    private String generarCodigo() {
        return String.format("%06d", aleatorio.nextInt(1000000));
    }

    // Igual que validarCodigo, pero con limite de intentos fallidos (cambio de correo)
    private CodigoVerificacion validarCodigoConIntentos(Integer solicitudId, String tipo, String valor) {
        CodigoVerificacion codigo = codigoRepo
                .findFirstBySolicitudIdAndTipoAndUsadoFalseOrderByIdDesc(solicitudId, tipo)
                .orElse(null);
        if (codigo == null) {
            throw new ArcoException(HttpStatus.BAD_REQUEST,
                    "No hay una verificacion pendiente para esa solicitud");
        }
        int intentos = codigo.getIntentos() == null ? 0 : codigo.getIntentos();
        if (intentos >= MAX_INTENTOS) {
            throw new ArcoException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Empieza el cambio de correo de nuevo.");
        }
        if (codigo.getExpiracion().isBefore(LocalDateTime.now())) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "El codigo expiro, empieza el cambio de correo de nuevo");
        }
        if (!codigo.getCodigo().equals(valor.trim())) {
            codigo.setIntentos(intentos + 1);
            codigoRepo.save(codigo);
            int quedan = MAX_INTENTOS - (intentos + 1);
            throw new ArcoException(HttpStatus.BAD_REQUEST, quedan > 0
                    ? "Codigo incorrecto. Te quedan " + quedan + " intento(s)."
                    : "Codigo incorrecto. Ya no te quedan intentos, empieza de nuevo.");
        }
        return codigo;
    }

    private SolicitudArco registrar(Usuario usuario, String tipoDerecho, String detalles, String estado) {
        SolicitudArco solicitud = new SolicitudArco();
        solicitud.setUsuario(usuario);
        solicitud.setTipoDerecho(tipoDerecho);
        solicitud.setDetalles(detalles);
        solicitud.setEstado(estado);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        if ("COMPLETADO".equals(estado)) {
            solicitud.setFechaRespuesta(LocalDateTime.now());
        }
        return solicitudRepo.save(solicitud);
    }

    private void cerrarSolicitud(SolicitudArco solicitud, String estado) {
        solicitud.setEstado(estado);
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudRepo.save(solicitud);
    }

    private void exigir(Object valor, String campo) {
        boolean vacio = valor == null || (valor instanceof String && ((String) valor).isBlank());
        if (vacio) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Falta el campo: " + campo);
        }
    }

    private void limitar(String valor, int maximo, String campo) {
        if (valor.length() > maximo) {
            throw new ArcoException(HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " no puede tener mas de " + maximo + " caracteres");
        }
    }
}
