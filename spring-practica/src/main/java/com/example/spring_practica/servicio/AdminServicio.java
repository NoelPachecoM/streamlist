package com.example.spring_practica.servicio;

import com.example.spring_practica.excepcion.ArcoException;
import com.example.spring_practica.modelo.BitacoraAdmin;
import com.example.spring_practica.modelo.EstadoLista;
import com.example.spring_practica.modelo.ListaUsuario;
import com.example.spring_practica.modelo.Rol;
import com.example.spring_practica.modelo.TipoTitulo;
import com.example.spring_practica.modelo.Titulo;
import com.example.spring_practica.modelo.Usuario;
import com.example.spring_practica.repositorio.BitacoraAdminRepositorio;
import com.example.spring_practica.repositorio.EstadoListaRepositorio;
import com.example.spring_practica.repositorio.ListaUsuarioRepositorio;
import com.example.spring_practica.repositorio.RolRepositorio;
import com.example.spring_practica.repositorio.TipoTituloRepositorio;
import com.example.spring_practica.repositorio.UsuarioRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel de Administracion: usuarios y roles, resenas y bitacora.
 *
 * Cada metodo recibe adminId (el id del usuario con sesion) y revisa EN LA BASE que ese usuario
 * tenga ROLE_ADMIN. Asi, si cambias el rol en pgAdmin, el permiso cambia al instante.
 * (Todavia no hay token de sesion: eso llega en la fase E.)
 */
@Service
public class AdminServicio {

    public static final String ROL_ADMIN = "ROLE_ADMIN";
    public static final String ROL_CLIENTE = "ROLE_CLIENTE";
    private static final int MAX_TEXTO_BITACORA = 300;

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final ListaUsuarioRepositorio lista;
    private final TipoTituloRepositorio tipos;
    private final EstadoListaRepositorio estados;
    private final BitacoraAdminRepositorio bitacora;

    public AdminServicio(UsuarioRepositorio usuarios, RolRepositorio roles, ListaUsuarioRepositorio lista,
                         TipoTituloRepositorio tipos, EstadoListaRepositorio estados,
                         BitacoraAdminRepositorio bitacora) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.lista = lista;
        this.tipos = tipos;
        this.estados = estados;
        this.bitacora = bitacora;
    }

    // ---------------- Usuarios y roles ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> usuarios(Integer adminId) {
        verificarAdmin(adminId);
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Usuario u : usuarios.findAll(Sort.by("id"))) {
            salida.add(aUsuario(u));
        }
        return salida;
    }

    @Transactional
    public Map<String, Object> cambiarRol(Integer adminId, Integer usuarioId, String rolNuevo) {
        Usuario admin = verificarAdmin(adminId);
        if (!ROL_ADMIN.equals(rolNuevo) && !ROL_CLIENTE.equals(rolNuevo)) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Rol invalido (ROLE_ADMIN o ROLE_CLIENTE)");
        }
        if (admin.getId().equals(usuarioId)) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "No puedes cambiar tu propio rol");
        }
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ArcoException(HttpStatus.NOT_FOUND, "El usuario no existe"));
        String rolAnterior = usuario.getRol() == null ? null : usuario.getRol().getNombre();
        if (rolNuevo.equals(rolAnterior)) {
            return aUsuario(usuario);
        }
        Rol rol = roles.findByNombre(rolNuevo).orElseThrow(() -> new ArcoException(
                HttpStatus.INTERNAL_SERVER_ERROR, "No existe el rol " + rolNuevo + " en la tabla roles"));
        usuario.setRol(rol);
        usuario = usuarios.save(usuario);

        registrar(admin, "Cambio de rol", "Cambió el rol de " + usuario.getNombre() + ": "
                + etiquetaRol(rolAnterior) + " → " + etiquetaRol(rolNuevo) + ".");
        return aUsuario(usuario);
    }

    // ---------------- Resenas ----------------

    /** Todas las resenas con texto (de titulos en "completado"), la mas reciente primero. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resenas(Integer adminId) {
        verificarAdmin(adminId);
        Map<Integer, String> clavesEstado = new HashMap<>();
        for (EstadoLista e : estados.findAll()) clavesEstado.put(e.getId(), e.getClave());
        Map<Integer, String> clavesTipo = new HashMap<>();
        for (TipoTitulo t : tipos.findAll()) clavesTipo.put(t.getId(), t.getClave());
        Map<Integer, Usuario> porId = new HashMap<>();
        for (Usuario u : usuarios.findAll()) porId.put(u.getId(), u);

        List<Map<String, Object>> salida = new ArrayList<>();
        for (ListaUsuario fila : lista.findAll(Sort.by(Sort.Direction.DESC, "actualizadoEn"))) {
            if (fila.getResena() == null || fila.getResena().isBlank()) continue;
            if (!MiListaServicio.COMPLETADO.equals(clavesEstado.get(fila.getEstadoId()))) continue;
            Usuario autor = porId.get(fila.getUsuarioId());
            Titulo t = fila.getTitulo();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", fila.getId());
            r.put("usuarioId", fila.getUsuarioId());
            r.put("usuario", autor == null ? "Usuario borrado" : autor.getNombre());
            r.put("correo", autor == null ? null : autor.getEmail());
            r.put("titulo", t.getNombre());
            r.put("tipo", clavesTipo.getOrDefault(t.getTipoId(), "pelicula"));
            r.put("tmdbId", t.getTmdbId());
            r.put("calificacion", fila.getCalificacion());
            r.put("resena", fila.getResena());
            r.put("fecha", fila.getActualizadoEn() == null ? null : fila.getActualizadoEn().toString());
            salida.add(r);
        }
        return salida;
    }

    /** Borra solo el texto de la resena (la calificacion se queda) y lo anota en la bitacora. */
    @Transactional
    public void eliminarTextoResena(Integer adminId, Integer filaId) {
        Usuario admin = verificarAdmin(adminId);
        ListaUsuario fila = lista.findById(filaId)
                .orElseThrow(() -> new ArcoException(HttpStatus.NOT_FOUND, "La resena ya no existe"));
        String texto = fila.getResena();
        if (texto == null || texto.isBlank()) {
            throw new ArcoException(HttpStatus.BAD_REQUEST, "Esa resena ya no tiene texto");
        }
        fila.setResena(null);
        lista.save(fila);

        Usuario autor = usuarios.findById(fila.getUsuarioId()).orElse(null);
        String recortado = texto.length() > MAX_TEXTO_BITACORA
                ? texto.substring(0, MAX_TEXTO_BITACORA) + "..." : texto;
        registrar(admin, "Reseña eliminada", "Eliminó la reseña de "
                + (autor == null ? "un usuario borrado" : autor.getNombre())
                + " sobre \"" + fila.getTitulo().getNombre() + "\". Texto: \"" + recortado + "\".");
    }

    // ---------------- Bitacora ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> bitacora(Integer adminId) {
        verificarAdmin(adminId);
        List<Map<String, Object>> salida = new ArrayList<>();
        for (BitacoraAdmin b : bitacora.findTop200ByOrderByFechaDesc()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", b.getId());
            r.put("fecha", b.getFecha() == null ? null : b.getFecha().toString());
            r.put("administrador", b.getAdminNombre());
            r.put("accion", b.getAccion());
            r.put("detalle", b.getDetalle());
            salida.add(r);
        }
        return salida;
    }

    // ---------------- Internos ----------------

    private Usuario verificarAdmin(Integer adminId) {
        if (adminId == null) {
            throw new ArcoException(HttpStatus.UNAUTHORIZED, "Inicia sesion como administrador");
        }
        Usuario admin = usuarios.findById(adminId)
                .orElseThrow(() -> new ArcoException(HttpStatus.UNAUTHORIZED, "Tu cuenta ya no existe"));
        if (admin.getRol() == null || !ROL_ADMIN.equals(admin.getRol().getNombre())) {
            throw new ArcoException(HttpStatus.FORBIDDEN, "Tu cuenta ya no es administradora");
        }
        return admin;
    }

    private void registrar(Usuario admin, String accion, String detalle) {
        BitacoraAdmin b = new BitacoraAdmin();
        b.setAdminId(admin.getId());
        b.setAdminNombre(admin.getNombre());
        b.setAccion(accion);
        b.setDetalle(detalle.length() > 2000 ? detalle.substring(0, 2000) : detalle);
        bitacora.save(b);
    }

    private Map<String, Object> aUsuario(Usuario u) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", u.getId());
        r.put("nombre", u.getNombre());
        r.put("correo", u.getEmail());
        r.put("rol", u.getRol() == null ? null : u.getRol().getNombre());
        r.put("activo", !Boolean.FALSE.equals(u.getActivo()));
        return r;
    }

    private String etiquetaRol(String rol) {
        if (ROL_ADMIN.equals(rol)) return "Administrador";
        if (ROL_CLIENTE.equals(rol)) return "Usuario";
        return "Sin rol";
    }
}
