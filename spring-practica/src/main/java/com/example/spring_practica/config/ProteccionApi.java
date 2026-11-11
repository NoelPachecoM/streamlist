package com.example.spring_practica.config;

import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.servicio.SesionServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Revisa el token de sesion ANTES de llegar a cualquier controlador de /api.
 *
 * Rutas publicas (sin token): login, registro, busqueda/detalle/tendencias de TMDB,
 * "olvide mi password" y las resenas publicas de un titulo.
 *
 * Rutas que siempre piden token: Mi lista, Mi sesion y Administracion (esta ademas pide ROLE_ADMIN).
 *
 * Rutas de la practica revisadas por el profesor (CRUD de tablas, ARCO, etc.):
 *   seguridad.modo-practica=true  (por defecto) -> abiertas como antes, para la coleccion de Postman.
 *   seguridad.modo-practica=false -> ARCO pide token; el CRUD de tablas pide token de administrador.
 */
@Component
public class ProteccionApi implements HandlerInterceptor {

    /** Nombre del atributo donde queda el usuario de la sesion para los controladores. */
    public static final String USUARIO = "usuarioSesion";

    private final SesionServicio sesiones;
    private final boolean modoPractica;

    public ProteccionApi(SesionServicio sesiones,
                         @Value("${seguridad.modo-practica:true}") boolean modoPractica) {
        this.sesiones = sesiones;
        this.modoPractica = modoPractica;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // preflight de CORS
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (!ruta.startsWith("/api/")) return true;

        Usuario usuario = sesiones.usuarioDe(token(request));
        if (usuario != null) request.setAttribute(USUARIO, usuario);

        // ---- Publicas ----
        if (ruta.equals("/api/auth/login")
                || ruta.startsWith("/api/auth/registro/")
                || ruta.startsWith("/api/tmdb/")
                || ruta.startsWith("/api/arco/olvide-password/")
                || ("GET".equalsIgnoreCase(request.getMethod()) && ruta.equals("/api/mi-lista/resenas"))) {
            return true;
        }

        // ---- Siempre con token (lo que usa la pagina) ----
        if (ruta.startsWith("/api/mi-lista") || ruta.startsWith("/api/auth/")) {
            return usuario != null || negar(response, 401, "Inicia sesion para continuar");
        }
        if (ruta.startsWith("/api/admin/")) {
            if (usuario == null) return negar(response, 401, "Inicia sesion para continuar");
            return esAdmin(usuario) || negar(response, 403, "Tu cuenta no es administradora");
        }

        // ---- Rutas de la practica ----
        if (modoPractica) return true;

        if (ruta.startsWith("/api/arco/")) {
            if (usuario == null) return negar(response, 401, "Inicia sesion para continuar");
            if (ruta.equals("/api/arco/datos")) {
                String correo = request.getParameter("correo");
                boolean propio = correo != null && correo.equalsIgnoreCase(usuario.getEmail());
                return propio || esAdmin(usuario) || negar(response, 403, "Solo puedes ver tus propios datos");
            }
            return true;
        }
        if (usuario == null) return negar(response, 401, "Inicia sesion como administrador");
        return esAdmin(usuario) || negar(response, 403, "Solo un administrador puede usar esta ruta");
    }

    private String token(HttpServletRequest request) {
        String encabezado = request.getHeader("Authorization");
        if (encabezado == null || !encabezado.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        return encabezado.substring(7).trim();
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario.getRol() != null && "ROLE_ADMIN".equals(usuario.getRol().getNombre());
    }

    private boolean negar(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
        return false;
    }
}
