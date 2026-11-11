package com.example.spring_practica.servicio;

import com.example.spring_practica.modelo.Sesion;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.SesionRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens de sesion. Al iniciar sesion se crea un token aleatorio (como una llave larga) que el
 * Angular manda en cada peticion con el encabezado "Authorization: Bearer <token>".
 * Spring ya no confia en el usuarioId que mande la pagina: sabe quien eres por el token.
 */
@Service
public class SesionServicio {

    private static final Duration DURACION = Duration.ofHours(24);

    private final SesionRepositorio sesiones;
    private final UsuarioRepositorio usuarios;
    private final SecureRandom aleatorio = new SecureRandom();

    public SesionServicio(SesionRepositorio sesiones, UsuarioRepositorio usuarios) {
        this.sesiones = sesiones;
        this.usuarios = usuarios;
    }

    /** Crea una sesion nueva y regresa el token (solo se muestra esta vez; en la base va su huella). */
    @Transactional
    public String crear(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.deleteByExpiraEnBefore(ahora); // limpieza de sesiones vencidas

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Sesion sesion = new Sesion();
        sesion.setTokenHash(huella(token));
        sesion.setUsuarioId(usuario.getId());
        sesion.setCreadaEn(ahora);
        sesion.setExpiraEn(ahora.plus(DURACION));
        sesiones.save(sesion);
        return token;
    }

    /** El usuario dueno del token, o null si el token no existe, vencio o el usuario ya no existe. */
    @Transactional
    public Usuario usuarioDe(String token) {
        if (token == null || token.isBlank() || token.length() > 200) return null;
        Sesion sesion = sesiones.findByTokenHash(huella(token)).orElse(null);
        if (sesion == null) return null;
        if (sesion.getExpiraEn() == null || sesion.getExpiraEn().isBefore(LocalDateTime.now())) {
            sesiones.delete(sesion);
            return null;
        }
        return usuarios.findById(sesion.getUsuarioId()).orElse(null);
    }

    @Transactional
    public void cerrar(String token) {
        if (token == null || token.isBlank()) return;
        sesiones.findByTokenHash(huella(token)).ifPresent(sesiones::delete);
    }

    /** Cierra todas las sesiones de un usuario (por ejemplo, al restablecer su contrasena). */
    @Transactional
    public void cerrarTodas(Integer usuarioId) {
        sesiones.deleteByUsuarioId(usuarioId);
    }

    private String huella(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
